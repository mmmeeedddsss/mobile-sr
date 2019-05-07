import tensorflow as tf
import train_opts as OPTS

from vgg19 import Vgg19

## general losses 
def create_loss_layer(input_hr_batch, output_hr_batch, img_loss_fn, 
                      discr_logits_real=None, discr_logits_fake=None):
    # add the image comparison loss
    cmp_loss = img_loss_fn(input_hr_batch, output_hr_batch, name='mse-loss')
    tf.add_to_collection(OPTS.MODEL_LOSSES, cmp_loss)

    # add the perceptual loss from vgg5,4 as in the SRGAN paper
    perceptual_loss_layer = VGG19PerceptualLossLayer(input_hr_batch, output_hr_batch)
    ploss = perceptual_loss_layer(name='perceptual-loss')
    tf.add_to_collection(OPTS.MODEL_LOSSES, ploss)

    # sanity check
    all_none = discr_logits_real is None and discr_logits_fake is None
    all_sth = not (discr_logits_real is None and discr_logits_fake is None)
    assert all_none or all_sth, 'GAN parameters should either all be None or should all exist!'

    # add GAN losses
    discr_exists = discr_logits_real is not None
    if discr_exists:
        # add discriminator losses
        discr_loss_real = tf.reduce_mean( # real loss
            tf.nn.sigmoid_cross_entropy_with_logits(
                logits=discr_logits_real, labels=tf.ones_like(discr_logits_real)),
            name='discr-loss-real')
        discr_loss_fake = tf.reduce_mean( # fake loss
            tf.nn.sigmoid_cross_entropy_with_logits(
                logits=discr_logits_fake, labels=tf.zeros_like(discr_logits_fake)),
            name='discr-loss-fake') 
        tf.add_to_collection(OPTS.DISCR_LOSSES, discr_loss_real)
        tf.add_to_collection(OPTS.DISCR_LOSSES, discr_loss_fake)

        # add adversarial loss
        adv_loss = tf.reduce_mean(
            tf.nn.sigmoid_cross_entropy_with_logits(
                logits=discr_logits_fake, labels=tf.ones_like(discr_logits_fake)))
        scaled_adv_loss = tf.multiply(OPTS.LOSS['adversarial_mult'], adv_loss,
                                      name='adversarial-loss')
        tf.add_to_collection(OPTS.MODEL_LOSSES, scaled_adv_loss)

    # add regularization losses
    for loss in tf.losses.get_regularization_losses(OPTS.MODEL_SCOPE):
        tf.add_to_collection(OPTS.MODEL_LOSSES, loss)
    if discr_exists:
        for loss in tf.losses.get_regularization_losses(OPTS.DISCR_SCOPE):
            tf.add_to_collection(OPTS.DISCR_LOSSES, loss)

    # print losses
    print()
    print()
    print('LOSSES:')
    print('******************************')
    print('Printing generator losses:')
    for l in tf.get_collection(OPTS.MODEL_LOSSES):
        print(l)
    if discr_exists:
        print('******************************')
        print('Printing discriminator losses:')
        for l in tf.get_collection(OPTS.DISCR_LOSSES):
            print(l)
    print('******************************')
    print()
    print()

    # sum and return the losses
    total_loss = tf.reduce_sum(tf.get_collection(OPTS.MODEL_LOSSES))
    if not discr_exists:
        return total_loss

    total_discr_loss = tf.reduce_sum(tf.get_collection(OPTS.DISCR_LOSSES))
    return total_loss, total_discr_loss

def mse_loss(input_hr_batch, output_hr_batch, name=None):
    return tf.multiply(
        OPTS.LOSS['mse_mult'],
        tf.reduce_mean(tf.squared_difference(input_hr_batch, output_hr_batch)), 
        name=name)

def to_vgg(input_batch):
    """ 
    A function to make input images compatible with the VGG19 network, 
    input images are brought from the [-1, 1] range to [0, 1]
    and are resized to 224x224
    """
    rescaled = (input_batch + 1.0) * 0.5
    resized = tf.image.resize_bicubic(rescaled, (224, 224))
    return resized

class VGG19PerceptualLossLayer:
    def __init__(self, input_hr_batch, output_hr_batch):
        # create the first VGG, from which we pass the reference HR images
        vgg_ihr = to_vgg(input_hr_batch)
        self.vgg1 = Vgg19()
        self.vgg1.build(vgg_ihr)
        # the second VGG, from which we pass our HR output
        vgg_ohr = to_vgg(output_hr_batch)
        self.vgg2 = Vgg19()
        self.vgg2.build(vgg_ohr)
        # take the Euclidean distance between the layers designated in the paper

    def __call__(self, name=None):
        # return the Euclidean distance between two layer outputs
        return tf.multiply(
            OPTS.LOSS['perceptual_mult'],    
            tf.reduce_mean(tf.squared_difference(self.vgg1.conv5_4, self.vgg2.conv5_4)), 
            name=name)

## regularizers
def get_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['l2_mult'])

def get_discr_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['discr_l2_mult'])
