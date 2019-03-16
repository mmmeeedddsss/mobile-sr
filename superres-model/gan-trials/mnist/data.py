import numpy as np

import idx


def center_images(data):
    # normalize and center the data
    return data.astype('float32') / 255.0 - 0.5

def get_set(data_path, label_path):
    images = center_images(
        np.expand_dims(
            idx.readfile(data_path, 3),
            axis=-1))
    labels = np.expand_dims(
        idx.readfile(label_path, 1),
        axis=-1)
    return images, labels

def get_training_set():
    return get_set('dataset/train-images-idx3-ubyte', 'dataset/train-labels-idx1-ubyte') 

def get_test_set():
    return get_set('dataset/t10k-images-idx3-ubyte', 'dataset/t10k-labels-idx1-ubyte')
