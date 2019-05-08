""" 
A module containing 'scheduler's for GAN training
a scheduler is simply an object that is called in
every iteration and returns a value depending on whether
the generator or discriminator should be trained
"""

class GoodfellowScheduler: 
    """ 
    Schedules just as explained in the original GAN paper,
    the discriminator is trained K times, and then the generator once
    """
    def __init__(self, k):
        self.k = k
        self.ctr = 0

    def reset(self):
        self.ctr = 0

    def train_discriminator(self):
        self.ctr = self.ctr + 1
        if self.ctr > self.k:
            self.ctr = 0
            return False
        return True

class SingleScheduler:
    """
    Schedules either the generator or the discriminator all the time, for debugging purposes
    """
    def __init__(self, schedule_discriminator):
        self.schedule_discriminator = schedule_discriminator

    def reset(self):
        pass

    def train_discriminator(self):
        return self.schedule_discriminator
