# https://youtu.be/QntADriNHuk
"""
Mask R-CNN - Multiclass - VGG style annotations in JSON format

For annotations, use one of the following programs: 
    https://www.makesense.ai/
    https://labelstud.io/
    https://github.com/Doodleverse/dash_doodler
    http://labelme.csail.mit.edu/Release3.0/
    https://github.com/openvinotoolkit/cvat
    https://www.robots.ox.ac.uk/~vgg/software/via/
    

"""

import os
import sys
import json
##import datetime
import numpy as np
import skimage.draw

from mrcnn.visualize import display_instances
from mrcnn.utils import extract_bboxes

from mrcnn.utils import Dataset
from matplotlib import pyplot as plt

from mrcnn.config import Config
from mrcnn.model import MaskRCNN


from mrcnn import model as modellib, utils



class CustomDataset(utils.Dataset):

    def load_custom(self, dataset_dir, subset):
        """Load a subset of the custom dataset.
        dataset_dir: Root directory of the dataset.
        subset: Subset to load: train or val
        """
        # Add classes according to the numbe of classes required to detect
        self.add_class("custom", 1, "kanan")
        self.add_class("custom",2,"kiri")

        # Train or validation dataset?
        assert subset in ["train", "val"]
        dataset_dir = os.path.join(dataset_dir, subset)

        # Load annotations
        # VGG Image Annotator (up to version 1.6) saves each image in the form:
        # { 'filename': '28503151_5b5b7ec140_b.jpg',
        #   'regions': {
        #       '0': {
        #           'region_attributes': {},
        #           'shape_attributes': {
        #               'all_points_x': [...],
        #               'all_points_y': [...],
        #               'name': 'polygon'}},
        #       ... more regions ...
        #   },
        #   'size': 100202
        # }
        # We mostly care about the x and y coordinates of each region
        # Note: In VIA 2.0, regions was changed from a dict to a list.
        annotations = json.load(open(os.path.join(dataset_dir, "labels\\anotasi.json")))
        annotations = list(annotations.values())  # don't need the dict keys

        # The VIA tool saves images in the JSON even if they don't have any
        # annotations. Skip unannotated images.
        annotations = [a for a in annotations if a['regions']]

        # Add images
        for a in annotations:
            # Get the x, y coordinaets of points of the polygons that make up
            # the outline of each object instance. These are stores in the
            # shape_attributes (see json format above)
            # The if condition is needed to support VIA versions 1.x and 2.x.
            polygons = [r['shape_attributes'] for r in a['regions'].values()]
            #labelling each class in the given image to a number

            custom = [s['region_attributes'] for s in a['regions'].values()]
            
            num_ids=[]
            #Add the classes according to the requirement
            for n in custom:
                try:
                    if n['label']=='kanan':
                        num_ids.append(1)
                    elif n['label']=='kiri':
                        num_ids.append(2)
                except:
                    pass

            # load_mask() needs the image size to convert polygons to masks.
            # Unfortunately, VIA doesn't include it in JSON, so we must read
            # the image. This is only managable since the dataset is tiny.
            image_path = os.path.join(dataset_dir, a['filename'])
            image = skimage.io.imread(image_path)
            height, width = image.shape[:2]

            self.add_image(
                "custom",
                image_id=a['filename'],  # use file name as a unique image id
                path=image_path,
                width=width, height=height,
                polygons=polygons,
                num_ids=num_ids)

    def load_mask(self, image_id):
        """Generate instance masks for an image.
       Returns:
        masks: A bool array of shape [height, width, instance count] with
            one mask per instance.
        class_ids: a 1D array of class IDs of the instance masks.
        """
        # If not a custom dataset image, delegate to parent class.
        image_info = self.image_info[image_id]
        if image_info["source"] != "custom":
            return super(self.__class__, self).load_mask(image_id)
        num_ids = image_info['num_ids']	
        #print("Here is the numID",num_ids)

        # Convert polygons to a bitmap mask of shape
        # [height, width, instance_count]
        info = self.image_info[image_id]
        mask = np.zeros([info["height"], info["width"], len(info["polygons"])],
                        dtype=np.uint8)
        for i, p in enumerate(info["polygons"]):
            # Get indexes of pixels inside the polygon and set them to 1
            rr, cc = skimage.draw.polygon(p['all_points_y'], p['all_points_x'])
            mask[rr, cc, i] = 1

        # Return mask, and array of class IDs of each instance. Since we have
        # one class ID only, we return an array of 1s
        num_ids = np.array(num_ids, dtype=np.int32)	
        return mask, num_ids#.astype(np.bool), np.ones([mask.shape[-1]], dtype=np.int32), 

    def image_reference(self, image_id):
        """Return the path of the image."""
        info = self.image_info[image_id]
        if info["source"] == "custom":
            return info["path"]
        else:
            super(self.__class__, self).image_reference(image_id)

dataset_train = CustomDataset()
dataset_train.load_custom("dataset\\", "train") 
dataset_train.prepare()
print('Train: %d' % len(dataset_train.image_ids))



# define image id
image_id = 0
# load the image
image = dataset_train.load_image(image_id)
# load the masks and the class ids
mask, class_ids = dataset_train.load_mask(image_id)

# display_instances(image, r1['rois'], r1['masks'], r1['class_ids'],
# dataset.class_names, r1['scores'], ax=ax, title="Predictions1")

# extract bounding boxes from the masks
bbox = extract_bboxes(mask)
# display image with masks and bounding boxes
display_instances(image, bbox, mask, class_ids, dataset_train.class_names)


# define a configuration for the model
class kakiConfig(Config):
	# define the name of the configuration
    NAME = "kaki_cfg"
    NUM_CLASSES = 1 + 2
    STEPS_PER_EPOCH = 100
    DETECTION_MIN_CONFIDENCE = 0.9 
    DETECTION_MAX_INSTANCES = 2
# prepare config
config = kakiConfig()
config.display() 




ROOT_DIR = os.path.abspath("C:\\Users\\0bbde\\Skripsi\\Mask_RCNN")
# Import Mask RCNN
sys.path.append(ROOT_DIR)  # To find local version of the library
# Directory to save logs and trained model
DEFAULT_LOGS_DIR = os.path.join(ROOT_DIR, "logs")

# Path to trained weights file
COCO_WEIGHTS_PATH = os.path.join(ROOT_DIR, "coco_weights\\mask_rcnn_coco.h5")



########################
#Weights are saved to root D: directory. need to investigate how they can be
#saved to the directory defined... "logs_models"

###############

# define the model
model = MaskRCNN(mode='training', model_dir=DEFAULT_LOGS_DIR, config=config)
# load weights (mscoco) and exclude the output layers
model.load_weights(COCO_WEIGHTS_PATH, by_name=True, exclude=["mrcnn_class_logits", "mrcnn_bbox_fc",  "mrcnn_bbox", "mrcnn_mask"])
# train weights (output layers or 'heads')
model.train(dataset_train, dataset_train, learning_rate=config.LEARNING_RATE, epochs=5, layers='heads')

############################
#INFERENCE

###################################################
from mrcnn.model import load_image_gt
from mrcnn.model import mold_image
from mrcnn.utils import compute_ap
from numpy import expand_dims
from numpy import mean
from matplotlib.patches import Rectangle


# define the prediction configuration
class PredictionConfig(Config):
	# define the name of the configuration
    NAME = "kaki_cfg"
	# number of classes (background + Blue kakis + Non Blue kakis)
    NUM_CLASSES = 1 + 2
	# Set batch size to 1 since we'll be running inference on
    # one image at a time. Batch size = GPU_COUNT * IMAGES_PER_GPU
    GPU_COUNT = 1
    IMAGES_PER_GPU = 1
    DETECTION_MIN_CONFIDENCE=0.9
    DETECTION_MAX_INSTANCES=2
 
# calculate the mAP for a model on a given dataset
def evaluate_model(dataset, model, cfg):
	APs = list()
	for image_id in dataset.image_ids:
		# load image, bounding boxes and masks for the image id
		image, image_meta, gt_class_id, gt_bbox, gt_mask = load_image_gt(dataset, cfg, image_id, use_mini_mask=False)
		# convert pixel values (e.g. center)
		scaled_image = mold_image(image, cfg)
		# convert image into one sample
		sample = expand_dims(scaled_image, 0)
		# make prediction
		yhat = model.detect(sample, verbose=0)
		# extract results for first sample
		r = yhat[0]
		# calculate statistics, including AP
		AP, _, _, _ = compute_ap(gt_bbox, gt_class_id, gt_mask, r["rois"], r["class_ids"], r["scores"], r['masks'])
		# store
		APs.append(AP)
	# calculate the mean AP across all images
	mAP = mean(APs)
	return mAP
 



#####################################################################################################################
cfg = PredictionConfig()
model = MaskRCNN(mode='inference', model_dir='logs', config=cfg)
model.load_weights('C:\\Users\\0bbde\\Skripsi\\Mask_RCNN\\logs\\epoch4\\mask_rcnn_kaki_cfg_0002.h5', by_name=True)
train_mAP = evaluate_model(dataset_train, model, cfg)
print("Train mAP: %.3f" % train_mAP)
#################################################



##############################################################
#
#   Mean Average Precision Validasi
#
##############################################################
cfg = PredictionConfig()
model = MaskRCNN(mode='inference', model_dir='logs', config=cfg)
model.load_weights('C:\\Users\\0bbde\\Skripsi\\Mask_RCNN\\logs\\epoch4\\mask_rcnn_kaki_cfg_0002.h5', by_name=True)
dataset_test = CustomDataset()
dataset_test.load_custom("dataset\\", "val") 
dataset_test.prepare()
testing_mAP = evaluate_model(dataset_test, model, cfg)
print("Testing mAP: %.3f" % testing_mAP)

#################################################
import cv2
import math as m

def calculate_real_size(kakis_img):

    detected = model.detect([kakis_img])
    results = detected[0]
    class_names = ['BG', 'kanan', 'kiri']
    display_instances(kakis_img, results['rois'], results['masks'], 
                      results['class_ids'], class_names, results['scores'])
    object_count = len(results["class_ids"])

    # Iterate over each detected mask
    object_count = len(results["class_ids"])
    for i in range(object_count):
        mask = results['masks'][:, :, i].astype(np.uint8)

        # Find contours to extract boundary points
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        # Select the largest contour if there are multiple contours
        largest_contour = max(contours, key=cv2.contourArea)

        # Extract boundary points from the contour
        boundary_points = largest_contour.squeeze()

        # Calculate pairwise distances between boundary points
        distances = []
        for i in range(len(boundary_points)):
            for j in range(i + 1, len(boundary_points)):
                dist = np.linalg.norm(boundary_points[i] - boundary_points[j])
                distances.append(dist)

        # Find the maximum distance
        max_distance = max(distances) / 37.79527559055118 ##/ 31.12392 #/ 28.35##/ 37.79527559055118
        max_pixel = max(distances)
        print(f"Longest distance from mask: {max_distance}cm, {max_pixel}pixel")
        ##NORMAL_ANGLE = m.radians(90)
        ##TAKEN_ANGLE = m.radians(91)
        ##FOCAL_LENGTH = 34
        ##DISTANCE = 66
        ##rumus = (DISTANCE*max_distance)/(FOCAL_LENGTH*m.cos(NORMAL_ANGLE-TAKEN_ANGLE))
        ##print(f"REAL SIZE : {rumus}")
        ##print(f"NORMAL = {NORMAL_ANGLE}\nTAKEN = {TAKEN_ANGLE}\nFOCAL = {FOCAL_LENGTH}\nDISTANCE = {DISTANCE}\n\n")
#################################################
#Test on a single image
for i in range(30):
    kakis_img = skimage.io.imread("dataset/val/test{0}.jpg".format(i+1))#i+1))
    print(f"[{i}]")
    calculate_real_size(kakis_img)
        
#################################################
kakis_img = skimage.io.imread("uploads/20230605114257_image.jpg")#i+1))
calculate_real_size(kakis_img)

