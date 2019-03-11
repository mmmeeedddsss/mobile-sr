import tensorflow as tf
import train_opts as OPTS

## general losses 
def create_loss_layer(input_hr_batch, output_hr_batch, img_loss_fn, 
                      discr_real_output=None, discr_fake_output=None):
    # add the image comparison loss
    cmp_loss = img_loss_fn(input_hr_batch, output_hr_batch)
    tf.add_to_collection(OPTS.MODEL_LOSSES, cmp_loss)

    # add GAN losses
    assert (discr_real_output is not None and discr_fake_output is not None) or \
           (discr_real_output is None and discr_fake_output is None), \
           'Discr outputs should both exist together or not exist at all!'
    discr_exists = discr_real_output is not None
    if discr_exists:
        # add adversarial loss
        adv_loss = adversarial_loss(discr_fake_output)
        tf.add_to_collection(OPTS.MODEL_LOSSES, adv_loss)
        # add discriminator losses
        real_loss, fake_loss = discriminator_losses(discr_real_output, discr_fake_output)
        tf.add_to_collection(OPTS.DISCR_LOSSES, real_loss)
        tf.add_to_collection(OPTS.DISCR_LOSSES, fake_loss)

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
    total_discr_loss = tf.reduce_sum(tf.get_collection(OPTS.DISCR_LOSSES)) if discr_exists else None
    return total_loss, total_discr_loss

def mse_loss(input_hr_batch, output_hr_batch):
    return tf.reduce_mean(tf.squared_difference(input_hr_batch, output_hr_batch), name='mse-loss')


## discriminator losses
def discriminator_losses(discr_real_output, discr_fake_output):
    # the discriminator should detect real HR images as real
    real_loss = tf.reduce_mean(tf.nn.sigmoid_cross_entropy_with_logits(
        labels=tf.ones_like(discr_real_output),
        logits=discr_real_output),
        name='discr-real-loss')
    # ... and fake HR images as fake
    fake_loss = tf.reduce_mean(tf.nn.sigmoid_cross_entropy_with_logits(
        labels=tf.zeros_like(discr_fake_output),
        logits=discr_fake_output),
        name='discr-fake-loss')
    return real_loss, fake_loss 

def adversarial_loss(discr_fake_output):
    # the generator network wants the discriminator to detect
    # the generated (fake) HR images as real
    adv_loss = tf.reduce_mean(tf.nn.sigmoid_cross_entropy_with_logits(
        labels=tf.ones_like(discr_fake_output),
        logits=discr_fake_output))
    scaled_adv_loss = tf.multiply(OPTS.LOSS['adversarial_mult'], adv_loss,
                                  name='adversarial-loss')
    return scaled_adv_loss



## regularizers
def get_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['l2_mult'])

def get_discr_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['discr_l2_mult'])
