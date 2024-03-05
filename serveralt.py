import os
import io
import sys
import json
import base64
import cv2
import time
import subprocess
import numpy as np
import skimage.draw
import requests as rq
from mrcnn.config import Config
from mrcnn.model import MaskRCNN
from mrcnn import model as modellib, utils
from flask import Flask, request, jsonify
from PIL import Image
from werkzeug.utils import secure_filename

ROOT_DIR = os.path.abspath("C:\\Users\\0bbde\\Skripsi\\Mask_RCNN")
# Import Mask RCNN
sys.path.append(ROOT_DIR)  # To find local version of the library
# Directory to save logs and trained model
DEFAULT_LOGS_DIR = os.path.join(ROOT_DIR, "logs")

# Path to trained weights file
COCO_WEIGHTS_PATH = os.path.join(ROOT_DIR, "coco_weights\\mask_rcnn_coco.h5")

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
    
cfg = PredictionConfig()
model = MaskRCNN(mode='inference', model_dir='logs', config=cfg)
model.load_weights('C:\\Users\\0bbde\\Skripsi\\Mask_RCNN\\logs\\epoch4\\mask_rcnn_kaki_cfg_0002.h5', by_name=True)

def calculate_real_size(kakis_img):
    detected = model.detect([kakis_img])
    results = detected[0]
    class_names = ['BG', 'kanan', 'kiri']   
    object_count = len(results["class_ids"])
    results_list = []

    # Iterate over each detected mask
    for k in range(object_count):
        mask = results['masks'][:, :, k].astype(np.uint8)

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
        y1, x1, y2, x2 = results['rois'][k]
        trimmed_image = kakis_img[y1:y2, x1:x2]
        
        max_distance = max(distances) ##/ 31.12392
        _, image_buffer = cv2.imencode('.jpg', trimmed_image)
        trimmed_image_jpg = base64.b64encode(image_buffer).decode('utf-8')
        
        results_dict = {
            'kaki': class_names[results["class_ids"][k]],
            'panjang': max_distance,
            'gambar': trimmed_image_jpg
        }

        # Append the results dictionary to the list
        results_list.append(results_dict)

    # Create the final JSON object
    results_json = json.dumps(results_list)

    return results_json
def check_serveo_status():
    try:
        response = rq.get('http://serveo.net')
        return response.status_code == 200
    except requests.exceptions.RequestException:
        return False
        
def connect_serveo():
    ssh_command = 'ssh -o ServerAliveInterval=60 -R skripsi:80:127.0.0.1:5000 serveo.net'
    localtunnel_command = 'lt --port 5000 -s skripsi'

    serveo_status = check_serveo_status()
    if serveo_status:
        print("Serveo is running")
        subprocess.Popen(['cmd', '/k', ssh_command], shell=True)
    else:
        print("Serveo is down, using LocalTunnel")
        subprocess.Popen(['cmd', '/k', localtunnel_command], shell=True)

# Call the connect_serveo function to establish the SSH connection

app = Flask(__name__)

@app.route('/upload', methods=['POST'])
def upload():
    # Proses argumen foto di sini
    if 'photo' not in request.files:
       return 'No photo uploaded', 400

    file = request.files['photo']

    # Check if the file is empty
    if file.filename == '':
       return 'Empty filename', 400

    image = Image.open(file)
    image = image.convert('RGB')
    img_array = np.array(image)

	# Specify the directory to save the photos
    save_directory = 'uploads'

    # Create the directory if it doesn't exist
    if not os.path.exists(save_directory):
        os.makedirs(save_directory)

    # Generate a unique filename for the image
    timestamp = time.strftime('%Y%m%d%H%M%S')
    filename = secure_filename(timestamp + '_' + file.filename)
    save_path = os.path.join(save_directory, filename)

    # Save the image to the specified directory
    image.save(save_path)

    result = calculate_real_size(img_array)
    # Kembalikan hasil sebagai respons dalam bentuk JSON
    return result

if __name__ == '__main__':
    connect_serveo()
    app.run(host='0.0.0.0', port=5000)

