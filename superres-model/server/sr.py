import numpy as np
import cv2
import tensorflow as tf


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

def bgr2rgbnb(img):
    rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    rgbnorm = rgb.astype('float32') * (1.0 / 255.0) - 0.5
    batched = np.expand_dims(rgbnorm, axis=0)
    return batched

def rgbnb2bgr(img):
    rgbnorm = np.squeeze(img)
    rgb = ((rgbnorm + 0.5) * 255).astype('uint8')
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    return bgr

def bytestream2img(byte_array):
    np_img = np.fromstring(byte_array, np.uint8)
    lr_img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    return lr_img

def img2bytestream(img):
    encoded = cv2.imencode('.png', img)
    retval, buf = encoded
    byte_array = buf.tobytes()
    return byte_array

def apply_sr(lr_data, model_dir):
    with tf.Session() as sess:
        lr_img = bytestream2img(lr_data)
        input_sym, output_sym = load_model(sess, model_dir)
        lr_norm = bgr2rgbnb(lr_img)
        hr_norm = sess.run(output_sym, feed_dict={input_sym: lr_norm})
        hr_img = rgbnb2bgr(hr_norm)
        hr_data = img2bytestream(hr_img)
        return hr_data
