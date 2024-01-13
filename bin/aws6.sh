#!/bin/bash

# script to start a ssh session on my Amazon EC2 instance that I run the App off
cd ~/AWS/KEYPAIRS

ssh -i "2024_Server.pem" ubuntu@ec2-18-130-65-36.eu-west-2.compute.amazonaws.com



