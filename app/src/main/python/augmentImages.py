import cv2
import os
import argparse
import imutils
import numpy as np
from tqdm import tqdm

parser = argparse.ArgumentParser()
parser.add_argument("path", help="path to the image directory")
parser.add_argument('-show', dest='show', action='store_true', default=False)
args = parser.parse_args()

assert os.path.isdir(args.path)
args.path = os.path.abspath(args.path)

angles = np.arange(-10, 10, 0.2)
#outputDirPath = os.path.join(args.path, os.path.basename(args.path) + f"_x{angles.shape[0]}_rotated")
outputDirPath = "/media/alex/Elements/captureUni/images/train_large_x100/"
if not os.path.exists(outputDirPath):
	os.mkdirs(outputDirPath)
assert os.path.exists(outputDirPath) and os.path.isdir(outputDirPath)
print(f"Going to save results in {outputDirPath}")

if(args.show):
	print("Quit program with by typing 'q' if image is shown!")


for file in tqdm(os.listdir(args.path)):

	file = os.path.abspath(os.path.join(args.path, file))
	if(os.path.isfile(file) and (file.endswith(".png") or file.endswith(".jpg"))):
		image = cv2.imread(file)
		for angle in angles:
			resized = imutils.resize(image, width=int(image.shape[1] * 1.12))
			rotated = imutils.rotate(resized, angle)
			
			h,w,c = (np.array(rotated.shape) - list(image.shape)) // 2
			new_image = rotated[h:-h,w:-w,:]
			
			s = os.path.basename(file).split("_")
			end = "_".join(s[2:])
			new_image_path = os.path.join(outputDirPath, f"{s[0]}_{s[1]}{angle}_{end}")
			cv2.imwrite(new_image_path, new_image)

			if(args.show):
				cv2.imshow(f"Original {image.shape}", image)
				cv2.imshow(f"Rotated and zooomed {new_image.shape}", new_image)
				if cv2.waitKey(0) == ord('q'):
					exit()
