    # TODO: calc with Python:
    # need to know:
    # precision: OutOfRetrievedSet(TP/(TP + FP))
    # for recall: FN (misses) --> number of place representations in dataset is needed in python script
    # TODO?: precision, recall, recall@k(-NN), precision@k(-NN)
    # averageRecall, averagePrecision, averageRecall@k(-NN), averagePrecision@k(-NN) --> to search for appropriate k
    # Single System wide number: mAP 


# AnnotationCSV:    queryNumb,resultCount,rank,label,x,y,yaw,pitch,distance
# ResultCSV:        resultCount,resultLabel,confidence,meanX,meanY,meanYaw,meanPitch
# QueryCSV:         queryNumb,inferenceTime,searchTime,trueLabel,trueX,trueY,trueYaw,truePitch

import numpy as np
import pandas as pd
import argparse
import configparser
from pprint import pprint, pformat
import os
import logging
import sys
import time
import datetime

summary_mAP_header =        ["test", "nCodebooks", "PCA","bridge","cross","floorADS","floorMCI","floorShort","head","lab","lift","officeB","officeD","T","mAP", "nNN"]
summary_precision_header =  ["test", "nCodebooks", "PCA","bridge","cross","floorADS","floorMCI","floorShort","head","lab","lift","officeB","officeD","T","mean"]

def calcMAP(k, doPrint=True):
    precisions = []
    averagePrecisions = {label:[] for label in queryAnnotations.trueLabel}
    
    pd.options.mode.chained_assignment = None  # default='warn'
    for queryNumb, ranking in resultAnnotations.groupby("queryNumb"):
        
        trueLabel = queryAnnotations.loc[queryNumb].trueLabel
        ranking = ranking.iloc[:k]
        ranking["isTP"] = (ranking["label"] == trueLabel)
        ranking["tp"] = ranking.isTP.astype(int).cumsum()
        ranking["fp"] = (~ranking.isTP).astype(int).cumsum()
        ranking["precision"] = ranking.tp / (ranking.tp + ranking.fp)
        tps = ranking[ranking.isTP]
        avgPrecision = tps.precision.mean() if not tps.empty else 0
        averagePrecisions[trueLabel].append(avgPrecision)
    pd.options.mode.chained_assignment = 'warn' # default='warn'    

    mAP = np.concatenate(list(averagePrecisions.values())).mean()
    classMAP = {label: np.array(x).mean() for label, x in averagePrecisions.items()}
    result = pd.DataFrame(data=classMAP, index=[0])
    result["mAP"] = mAP
    result["nNN"] = k
    return result

if __name__ == "__main__":
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='[%(levelname)s] %(message)s')
    log = logging.getLogger("stdout")

    parser = argparse.ArgumentParser(description='Result analysis for IR system: mAP, parametrisation, scalability')
    # parser.add_argument("indexDumpCSVPath", type=str)
    parser.add_argument("csvFilesDir", type=str)
    
	
    args = parser.parse_args()

    # assert os.path.exists(args.indexDumpCSVPath), "Directory does not exist: " + args.indexDumpCSVPath
    # assert os.path.isfile(args.indexDumpCSVPath) and os.path.basename(args.indexDumpCSVPath).endswith("csv"), "Is not a CSV file: " + args.indexDumpCSVPath
    assert os.path.exists(args.csvFilesDir), "Path does not exist: " + args.csvFilesDir
    assert os.path.isdir(args.csvFilesDir), "Is not a directory: " + args.csvFilesDir
    
    print(f"[{str(datetime.datetime.now()).split('.')[0]}] Loading files...")
    configPath, resultAnnotationCSVPath, resultCSVPath, queryAnnotationCSVPath = (None, None, None, None)
    for file in os.listdir(args.csvFilesDir):
        file = os.path.abspath(os.path.join(args.csvFilesDir, file))
        if file.endswith(".config"):
            configPath = file
        if file.endswith("result_annotations.csv"):
            resultAnnotationCSVPath = file
        if file.endswith("results.csv"):
            resultCSVPath = file
        if file.endswith("query_annotations.csv"):
            queryAnnotationCSVPath = file
    

    assert not None in [configPath, resultAnnotationCSVPath, resultCSVPath, queryAnnotationCSVPath], "Missing files in test output directory (.conf, result_annotations.csv, results.csv or query_annotations.csv)"
    
    

    # read config to see if mAP (NQueriesForResult = 1) or result analysis
    config = configparser.ConfigParser()
    config.optionxform = str 
    config.readfp(open(configPath))
    pprint({section: dict(config[section]) for section in config.sections()})

    baseFilename = os.path.basename(configPath)[:-len(".config")]
    nQueriesForResult = int(config["Search"]["nQueriesForResult"])
    nNN = int(config["Search"]["nNN"])
    testName = config["Test"]["testName"]
    nCodebooks = len(config["Vectorisation"]["codebookSizes"].split(","))
    nPCA = config["Vectorisation"]["projectionLength"]
   
queryAnnotations = pd.read_csv(queryAnnotationCSVPath, header=0)
resultAnnotations = pd.read_csv(resultAnnotationCSVPath, header=0)
resultsPR = pd.read_csv(resultCSVPath, header=0)

doSummary = True
if doSummary:
    summaryMAP_path = "/home/alex/Documents/Git/multimedia-indexing/captureUni_rotated/test_results/deployTests/summary_mAP.csv"
    summaryMAP = pd.read_csv(summaryMAP_path, header=0)

    summaryPrecision_path = "/home/alex/Documents/Git/multimedia-indexing/captureUni_rotated/test_results/deployTests/summary_precision.csv"
    summaryPrecision = pd.read_csv(summaryPrecision_path, header=0)

print(f"[{str(datetime.datetime.now()).split('.')[0]}] Starting mAP Analysis...")
start_s = time.time()
# 1. Test: mAP # calculate precision for each ranking entry --> take the average of that --> do this for each query and take the mean of the averages
do_mAP_Analysis = (nQueriesForResult == 1)
if(do_mAP_Analysis):
    resultMAPs = pd.concat([calcMAP(k) for k in range(1, nNN+1)],ignore_index=True)
    print(resultMAPs)
    resultMAPs = resultMAPs.sort_values(by=['mAP'], ascending=False)
    resultMAPs.to_csv(os.path.join(args.csvFilesDir, baseFilename + f"_analysis_mAP.csv"), index=False)
    
    resultMAPs["nCodebooks"] = nCodebooks
    resultMAPs["PCA"] = nPCA
    resultMAPs["test"] = testName
    if doSummary:
        summaryMAP = summaryMAP.append(resultMAPs.iloc[0])
        summaryMAP.to_csv(summaryMAP_path, index=False)
    

print(f"mAP test took {time.time() - start_s} s")

print(f"[{str(datetime.datetime.now()).split('.')[0]}] Starting precision test...")
start_s = time.time()
# 2.Test: for the system with best mAP run tests with varying nQueriesForResult and analyse the precision by comparing resultLabel and trueLabel
label_tp_fp_fn = {label:[0,0] for label in set(queryAnnotations.trueLabel)}

# count different classes in index for Recall
# with open(args.indexDumpCSVPath) as f:        
#     for line in f:
#         label = "_".join(line.split(",")[0].split("_")[2:-4])
#         if label in label_tp_fp_fn:
#             label_tp_fp_fn[label][2] += 1

# precision and recall for result
i = 0
for index, query in queryAnnotations.iterrows():
    if ((index+1) % nQueriesForResult) == 0:
        if query.trueLabel == resultsPR.iloc[i].resultLabel:
            label_tp_fp_fn[query.trueLabel][0] += 1
            # label_tp_fp_fn[query.trueLabel][2] -= 1
        else:
            label_tp_fp_fn[query.trueLabel][1] += 1
        i += 1

print(label_tp_fp_fn)
tp_fp_sum = np.sum(np.array(list(label_tp_fp_fn.values())), axis=0)
resultsPR_analysis = pd.DataFrame(columns=label_tp_fp_fn.keys())
resultsPR_analysis.loc["precision"] = [v[0]/(v[0]+v[1]) if v[0]+v[1] != 0 else 0 for k,v in label_tp_fp_fn.items()]
# resultsPR_analysis.loc["recall"] = [v[0]/(v[0]+v[2]) for k,v in label_tp_fp_fn.items()]
resultsPR_analysis["mean"] = resultsPR_analysis.mean(axis=1)

print(resultsPR_analysis)
resultsPR_analysis.to_csv(os.path.join(args.csvFilesDir, baseFilename + "_analysis_resultPrecision.csv"), index=False)

if doSummary:
    resultsPR_analysis["nCodebooks"] = nCodebooks
    resultsPR_analysis["PCA"] = nPCA
    resultsPR_analysis["test"] = testName
    summaryPrecision = summaryPrecision.append(resultsPR_analysis)
    summaryPrecision.to_csv(summaryPrecision_path, index=False)

print(f"Precision test took {time.time() - start_s} s")