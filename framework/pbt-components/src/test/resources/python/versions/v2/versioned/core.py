import os

from .label import LABEL


class VersionError(Exception):
    pass


def version(s):
    return LABEL + ":" + s


def reject(s):
    raise VersionError(s)


def working_directory(_):
    return os.getcwd()
