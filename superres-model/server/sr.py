import numpy as np
import cv2
import tensorflow as tf
import os

# Disable deprecation warnings
# tf.logging.set_verbosity(tf.logging.FATAL)
# os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

# loads model with given path
def load_model(sess, model_path):
    tf.saved_model.loader.load(
        sess,
        [tf.saved_model.tag_constants.SERVING],
        model_path)
    # get the symbolic variables through the default graph
    dg = tf.get_default_graph()
    input_sym = dg.get_tensor_by_name('input_image:0')
    output_sym = dg.get_tensor_by_name('output_image:0')
    return input_sym, output_sym

# convert between color spaces
# and data normalization
def bgr2rgbnb(img):
    rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    rgbnorm = rgb.astype('float32') / 127.5 - 1.0
    batched = np.expand_dims(rgbnorm, axis=0)
    return batched

# convert between color spaces
# and data normalization
def rgbnb2bgr(img):
    rgbnorm = np.clip(np.squeeze(img), -1.0, 1.0)
    rgb = ((rgbnorm + 1.0) * 127.5).astype('uint8')
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    return bgr

# decodes given bytestream to
# OpenCV Mat object
def bytestream2img(byte_array):
    np_img = np.fromstring(byte_array, np.uint8)
    lr_img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    return lr_img

# encodes given OpenCV Mat object to
# file with given format and returns the final bytearray
# default: PNG
def img2bytestream(img, file_format='.png'):
    encoded = cv2.imencode(file_format, img)
    retval, buf = encoded
    byte_array = buf.tobytes()
    return byte_array

# Given low-res image data and model path
# returns high-res image data
# conversions between binary data and Mat objects
# are done internally
def apply_sr(lr_data, model_dir):
    lr_img = bytestream2img(lr_data)
    hr_img = apply_sr_img(lr_img, model_dir)
    hr_data = img2bytestream(hr_img)
    return hr_data

# Given low-res image Mat object and model path
# returns high-res image Mat object
def apply_sr_img(lr_img, model_vars):
    sess, input_sym, output_sym = model_vars
    lr_norm = bgr2rgbnb(lr_img)
    hr_norm = sess.run(output_sym, feed_dict={input_sym: lr_norm})
    hr_img = rgbnb2bgr(hr_norm)
    return hr_img
