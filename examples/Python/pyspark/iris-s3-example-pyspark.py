# iris-s3-example-pyspark.py
#
# Ran with Spark 4.0.0, Python 3.9.7, Java 17
# export SPARK_HOME=/opt/spark-4.0.0-bin-hadoop3
# export PYTHONPATH=/opt/symetry/python
# export PYSPARK_DRIVER_PYTHON="$HOME/uv-env/bin/python"
# export PYSPARK_DRIVER_PYTHON_OPTS=
# export JAVA_HOME=$(/usr/libexec/java_home -v 17)
# export AWS_ACCESS_KEY="$aws_access_key_1"
# export AWS_SECRET_KEY="$aws_secret_key_1"
# $SPARK_HOME/bin/spark-submit --master local[1] --packages org.apache.hadoop:hadoop-aws:3.3.6,com.amazonaws:aws-java-sdk-bundle:1.11.1026 --jars "/opt/symetry/lib/sym-spark-assembly-Scala_2_13.jar,/opt/spark-4.0.0-bin-hadoop3/jars/scala-library-2.13.16.jar" --driver-java-options -Dsym.lic.loc=/opt/symetry/sym.lic iris-s3-example-pyspark.py

import os
import sys
import pyspark
from pyspark.context import SparkContext
from pyspark.context import SparkConf
from pyspark.sql import SQLContext, HiveContext
from pyspark.storagelevel import StorageLevel

print("amazonExample.py start")

conf = SparkConf()
conf.setAppName('amazonExample')
sc = SparkContext(conf=conf)

gateway         = sc._gateway
sym             = gateway.jvm.com.sml.shell

# Find the access keys for EC2.
awsAccessKeyId = os.environ['AWS_ACCESS_KEY']
awsSecretAccessKey = os.environ['AWS_SECRET_KEY']
# print("awsAccessKeyId=" + awsAccessKeyId)
# print("awsSecretAccessKey=" + awsSecretAccessKey)

sc._jsc.hadoopConfiguration().set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
sc._jsc.hadoopConfiguration().set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider")
sc._jsc.hadoopConfiguration().set("fs.s3a.access.key", awsAccessKeyId)
sc._jsc.hadoopConfiguration().set("fs.s3a.secret.key", awsSecretAccessKey)

# We need these in order to get rid of units in the default property values.
# From https://hadoop.apache.org/docs/stable/hadoop-aws/tools/hadoop-aws/performance.html:
# Units:
# 1. The default unit for all these options except for fs.s3a.threads.keepalivetime is milliseconds, unless a time suffix is declared.
# 2. Versions of Hadoop built with the AWS V1 SDK only support milliseconds rather than suffix values. If configurations are intended to apply across hadoop releases, you MUST use milliseconds without a suffix.
# 3. fs.s3a.threads.keepalivetime has a default unit of seconds on all hadoop releases.
sc._jsc.hadoopConfiguration().set("fs.s3a.connection.timeout", "60000")
sc._jsc.hadoopConfiguration().set("fs.s3a.connection.establish.timeout", "60000")
sc._jsc.hadoopConfiguration().set("fs.s3a.connection.request.timeout", "60000")
sc._jsc.hadoopConfiguration().set("fs.s3a.connection.keepalive.time", "60000")
sc._jsc.hadoopConfiguration().set("fs.s3a.threads.keepalivetime", "60000")
sc._jsc.hadoopConfiguration().set("fs.s3a.multipart.purge.age", "86400")

myrdd  = sc.textFile('s3a://sml-oregon/datasets/susy/SUSYmini.csv')
# Convert pyspark RDD to JavaRDD
# _to_java_object_rdd
myJavaRdd = myrdd._jrdd

# The first line of CSV file are the name of the attributes
attributeNames = myrdd.first().split(",")
# The attributeTypes has to be given
attributeTypes = ["B"]+["C"]*(len(attributeNames)-1)

# The IP address of the host if empty, project is not persisted. (Not Persisted Here)
# sym.SymShellConfig.set("RedisHost","charm")
# sym.SymShellConfig.set("RedisPort",6379)

# 1) Create the Project here
projectName     = "susyExampleInPython"
userName        = "c1"
projectType     =  0         # 0: Using CPU, 11:using GPU
p               = sym.PySparkShellSymetryProject(userName,projectName, projectType)

# 2) Learn the RDD
# sc : is the SparkContext which is automatically generated (sc._jsc: is its Java version)
p.learn(sc._jsc, myJavaRdd, attributeNames, attributeTypes, None)


# 3)  some data exploration (univariate, and bivariate Statistics)
print(p.univariate(4)) # exploration , see whether the project has been built correctly
print(p.univariate("lepton-2-pT")) # You may pass the name of the attribute as well
stats = p.univariate("lepton-2-pT")

# Measuring some univariate statistics
x = range(1,len(attributeNames))
attrVariance=[p.univariate(i-1)["variance"] for i in x]
attrMean=[p.univariate(i-1)["mean"] for i in x]

print(p.bivariate("lepton-2-pT","lepton-2-phi")["linCorr"])

# 4) Perform PCA
e1 = p.pca(range(1,19)) # returns a Tuple[eigenvalues,eigenvectors]
print(e1["EigenValues"][0])                                   # the first eigen-values
print(e1["EigenVectors"][0])                                  # the first eigen-vectors

e2 = p.pca([
    "lepton-1-pT",
    "lepton-1-eta", "lepton-1-phi",
    "lepton-2-pT", "lepton-2-eta"])

# 5) Build model
tar   = [0]                              # The forth Attribute is used as Target
input = range(1,19)                      # The first four attribute used as as Input

p.buildModel(input, tar, "lsvm", "mySvmModel")       # The Model is know built

# One row of data (Attributes are comma separated)
# Here in this model, the first Attribute is the Target,
# we put an arbitrary value as it will be ignored in the prediction

# The response for this example should be 1
test1 = ["-1","1.667973", "0.0641906","-1.2251714",
         "0.506102", "-0.3389389", "1.6725428",
         "3.475464", "-1.2191363", "0.0129545",
         "3.775173", "1.0459771",  "0.5680512",
         "0.481928", "0.0000000", "0.4484102",
         "0.205355", "1.3218934", "0.3775840"]

df1 = sym.PyDataFrame() # The dataframe has to be type PyDataFrame
df1.setAttributeNames(attributeNames)
df1.setAttributeTypes(attributeTypes)
df1.addTuple(test1)
results1     = p.predict(df1,"mySvmModel")
print(results1)

# The response for this example should be 0
test2 = ["-1","1.001869","-0.471788","0.555614",
         "1.233368","1.255548","-1.052491",
         "0.437615","-1.333052","0.326858",
         "-0.111678","1.435708","0.755201",
         "0.466779","0.454541","1.446331",
         "0.592259","1.325197","0.083014"]

df1.clear()
df1.addTuple(test2)
results2     = p.predict(df1,"mySvmModel")
print(results2)

# STEP 6) You can delete the Model to release the used memory
p.deleteModel("mySvmModel")

# STEP 7) Tou can delete the Project to release the used memory
p.deleteProject()

print("amazonExample.py end")
