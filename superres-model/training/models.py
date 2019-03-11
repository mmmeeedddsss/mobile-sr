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
    cl1 = tf.keras.layers.Conv2D(64, 9, padding='SAME')
    c1 = cl1(lr_resized)
    # second conv. layer
    cl2 = tf.keras.layers.Conv2D(32, 1, padding='SAME')
    c2 = cl2(c1)
    # final conv. layer
    cl3 = tf.layers.Conv2D(3, 5, padding='SAME')
    hr_batch = cl3(c2)
    return hr_batch

