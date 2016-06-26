#!/usr/bin/python
# requires an ApiUser (corehq.apps.api.models.ApiUser) on the remote_host with username/password given

import os
import shlex

import subprocess
from subprocess import PIPE
import sys

def submit_build(environ, host):
    target_url= host + "/builds/post/"

    command =  (
        'curl -v '
        '-H "Expect:" '
        '-F "artifacts=@{ARTIFACTS}" '
        '-F "username={USERNAME}" '
        '-F "password={PASSWORD}" '
        '-F "build_number={BUILD_NUMBER}" '
        '-F "version={VERSION}" '
        '{target_url}'
    ).format(target_url=target_url, **environ)

    p = subprocess.Popen(shlex.split(command), stdout=PIPE, stderr=None, shell=False)
    return command, p.stdout.read(), "" #p.stderr.read()


if __name__ == "__main__":

    variables = [
        "USERNAME",
        "PASSWORD",
        "ARTIFACTS",
        "REMOTE_HOST",
        "VERSION",
        "BUILD_NUMBER",
    ]
    args = sys.argv[1:]
    environ = None
    try:
        environ = dict([(var, os.environ[var]) for var in variables])
		
    except KeyError:
        if len(args) == len(variables):
            environ = dict(zip(variables, args))

    if environ:
    	hosts = environ['REMOTE_HOST'].split("+")
    	for host in hosts:
	        command, out, err = submit_build(environ, host)
	        print command
	        if out.strip():
	            print "--------STDOUT--------"
	            print out
	        if err.strip():
	            print "--------STDERR--------"
	            print err
    else:
        print("submit_build.py <%s>" % ("> <".join(variables)))
