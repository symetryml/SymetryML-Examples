package com.sml.examples.scala

/**
 * To run via spark-shell locally, for example:
 * Spark 3.0.2, Hadoop 3.2, Scala 2.12.10, Java 11
 *
 * Build sym-spark-assembly-Scala_2_12.jar at ScalaProjects/sym-shell:
 * ./gradlew clean shadowJar -PuseSpark3
 *
 * export SPARK_HOME=/opt/spark-3.0.2-bin-hadoop3.2
 * export JAVA_HOME=$(/usr/libexec/java_home -v 11)
 * export APP_JAR=/opt/symetry/lib/sym-spark-assembly-Scala_2_12.jar
 * export AWS_ACCESS_KEY="$aws_access_key_1"
 * export AWS_SECRET_KEY="$aws_secret_key_1"
 * SPARK_SHELL_CMD="com.sml.examples.scala.amazonMRExample.run(sc)"
 
 echo -e "$SPARK_SHELL_CMD" | $SPARK_HOME/bin/spark-shell --master local[*] --jars $APP_JAR --driver-java-options -Dsym.lic.loc=/opt/symetry/sym.lic \
   --packages \
    com.amazonaws:aws-java-sdk-bundle:1.11.375,\
    org.apache.hadoop:hadoop-aws:3.2.0
 
 *
 */


import com.rtlm.constants.CoreConstants._
import com.rtlm.json.DataFrame
import com.rtlm.util.AttributeTypes
import com.sml.shell.SparkShellSymetryProject
import com.sml.shell.SymShellConfig
import com.sml.shell.Util._

object amazonMRExample {
  /**
   * Create a Distributed SPARK Symetry Project using a dataset on Amazon S3.
   * @param sc It requires sc: SparkContext which is usually built in spark-shell
   */
  def run(sc:org.apache.spark.SparkContext, path:String="/home/ec2-user/sym-spark-examples/config/amazonS3Key.txt")={

    println("amazonExample.scala start")

    // Read Amazon S3 Credentials from the amazonS3Key.txt file
    // The file is structure as follow:
    // awsAccessKeyId=<the access key>
    // awsSecretAccessKey=<the secret key>
    //println(path)
    //val (awsAccessKeyIdTemp,awsSecretAccessKeyTemp) = readAmazonS3KeyFromFile(path)
    val awsAccessKeyId           = System.getenv("AWS_ACCESS_KEY")
    val awsSecretAccessKey       = System.getenv("AWS_SECRET_KEY")

    // or Instead define awsAccessKeyId, and awsSecretAccessKey
    val hadoopConf=sc.hadoopConfiguration
    hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
    hadoopConf.set("fs.s3a.access.key", awsAccessKeyId)
    hadoopConf.set("fs.s3a.secret.key", awsSecretAccessKey)

    // STEP 2) Create RDD (Resilient Distributed Data) from your data, Specify Attribute Names, and their Types.

    //the dataset can be on Amazon s3: For example s3n://sml-oregon
    val myrdd  = sc.textFile("s3a://sml-oregon/datasets/susy/SUSYmini.csv")
    val attributeNames = myrdd.first.split(",")         // The first line of CSV file are the name of the attributes
    val attributeTypes: Array[Char] =
      Array.concat(
        Array(AttributeTypes.TYPE_BINARY),
        Array.fill(attributeNames.length - 1)(AttributeTypes.TYPE_CONTINOUS)
      )

    /* Specify attribute types:
        AttributeTypes.TYPE_CONTINOUS
        AttributeTypes.TYPE_BINARY
        AttributeTypes.TYPE_STRING
        AttributeTypes.TYPE_LIST
        AttributeTypes.TYPE_IGNORE
        AttributeTypes.TYPE_CATEGORY
     */

    // STEP 3) Configuration, Any parameter that not specified below will be replaced bt their default values.
    //
    SymShellConfig.RedisHost = "" // The IP address of the host if empty, the project is not persisted.
    SymShellConfig.RedisPort = 6379
    // STEP 4) Create a distributed SymetryProject

    val projectName = "susyExample-TEST"
    val userName    = "c1"
    val projectType = 0 // 0: Using CPU, 11:using GPU
    val p = new SparkShellSymetryProject(
        userName,
        projectName,
        projectType)
    /*
     */
    p.learn(sc, myrdd, attributeNames, attributeTypes) // import the rdd in the distributed SPARK project

    //var theProject = p.getProject
    // STEP 5) a)Exploring the data, b) PCA, c) Building model, d) prediction

    // 5a) Exploring the data
    println("Univaraite Exploration of Attribute number 4")
    val stats1 = p.univariate(4) // exploration , see whether the project has been built correctly
    println(stats1)
    p.univariate("lepton-2-pT") // You may pass the name of the attribute as well

    println("Univaraite Exploration of the lepton-2-pT Attribute")
    val stats2 = p.univariate("lepton-2-pT") // You may pass the name of the attribute as well
    println(stats2)
    println("Skewness:")
    println(stats2(UNI_SKEWNESS))

    // 5b) PCA
    val eigens1 = p.pca((1 to 18).toArray) // returns a Map containing eigenvalues,eigenvectors
    // eigenValues                            : eigen-values
    // pc                                     : eigen-vectors

    // or pass the Attributes name
    val eigens2 = p.pca(Array(
      "lepton-1-pT",
      "lepton-1-eta", "lepton-1-phi",
      "lepton-2-pT", "lepton-2-eta"))

    // 5b) Build model
    val tar   = Array(0)                              // The forth Attribute is used as Target
    val input = (1 to 18).toArray                     // The first four attribute used as as Input

    p.buildModel(input, tar, "lsvm", "mySvmModel")       // The Model is know built

    // 5c) make predictions (There are different ways to make prediction

    // One row of data (Attributes are comma separated);
    // Here in this model, the first Attribute is the Target,
    // we put an arbitrary value as it will be ignored in the prediction

    // The response for this example should be 1
    val test1 = Array("-1","1.667973", "0.0641906","-1.2251714",
      "0.506102", "-0.3389389", "1.6725428",
      "3.475464", "-1.2191363", "0.0129545",
      "3.775173", "1.0459771",  "0.5680512",
      "0.481928", "0.0000000", "0.4484102",
      "0.205355", "1.3218934", "0.3775840")
    val df1 = new DataFrame
    df1.setAttributeNames(attributeNames)
    df1.setAttributeTypes(attributeTypes)
    df1.addTuple(test1)
    val res1     = p.predict(df1,"mySvmModel")
    println("Estimated response:" + res1(0))

    // The response for this example should be 0
    val test2 = Array("-1","1.001869","-0.471788","0.555614",
      "1.233368","1.255548","-1.052491",
      "0.437615","-1.333052","0.326858",
      "-0.111678","1.435708","0.755201",
      "0.466779","0.454541","1.446331",
      "0.592259","1.325197","0.083014")

    df1.clear
    df1.addTuple(test2)
    val res2     = p.predict(df1,"mySvmModel")
    println("Estimated response:" + res2(0))

    // STEP 6) You can delete the Model to release the used memory
    p.deleteModel("mySvmModel")

    // STEP 7) Tou can delete the Project to release the used memory
    p.deleteProject
    
    println("amazonExample.scala end")
  }
}

// **************************************
// ******** PLEASE RUN: *****************
// amazonExample.run(sc)
