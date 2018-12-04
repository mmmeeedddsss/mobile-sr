import tensorflow as tf

def srcnn_x2_weak(lr_batch):
    # apply a 2x rescaling transpose conv.
    lr_resized = tf.layers.conv2d_transpose(
        inputs=lr_batch,
        filters=3,
        kernel_size=3,
        strides=2,
        padding='same')
    # first conv. layer
    c1 = tf.layers.conv2d(lr_resized, 64, 9, padding='SAME')
    # second conv. layer
    c2 = tf.layers.conv2d(c1, 32, 1, padding='SAME')
    # final conv. layer
    hr_batch = tf.layers.conv2d(c2, 3, 5, padding='SAME')
    return hr_batch

