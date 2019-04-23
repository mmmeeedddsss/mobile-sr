""" a small module to provide utils for reading the MNIST IDX files """

import os
import struct

import numpy as np
from PIL import Image


def readfile(file_path, ndims):
    with open(file_path, 'rb') as f:
        f.read(4) # read the magic number
        dims = []
        for _ in range(ndims):
            btes = f.read(4) # read 4 bytes
            dim = struct.unpack('>I', btes)[0] # unpack as unsigned int
            dims.append(dim) # add to dims
        data_1d = np.fromfile(f, dtype='uint8') # read the rest of the file as 1D
    shaped_data = data_1d.reshape(dims) # reshape the data as necessary
    return shaped_data

def write_grayscale_imgs(data, dirname):
    assert len(data.shape) == 3, 'Data should be in the format NxWxH'
    for i, img in enumerate(data):
        img_path = os.path.join(dirname, str(i) + '.png')
        img_obj = Image.fromarray(img)
        img_obj.save(img_path, 'PNG')

