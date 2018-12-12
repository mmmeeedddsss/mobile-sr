import tensorflow as tf

def srcnn_x2_weak(lr_batch):
    # apply nn resize (as it is supported in TFLite)
    hr_shape = 2 * tf.shape(lr_batch)[1:3]
    lr_resized = tf.image.resize_nearest_neighbor(lr_batch, hr_shape)
    # save the name of the resized_image for model modification
    lr_resized = tf.identity(lr_resized, name='resized_image')
    # add a summary for resized
    tf.summary.image('resized_image', lr_resized)
    # first conv. layer
    c1 = tf.layers.conv2d(lr_resized, 64, 9, padding='SAME')
    # second conv. layer
    c2 = tf.layers.conv2d(c1, 32, 1, padding='SAME')
    # final conv. layer
    hr_batch = tf.layers.conv2d(c2, 3, 5, padding='SAME')
    return hr_batch

