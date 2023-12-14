@Library('pipeline-commons') _

mavenPipeline {
    memPRD = [ memMrp:60, memRes:512, memMax:512]
    memQAS = [ memMrp:60, memRes:512, memMax:512]
    memHML = [ memMrp:60, memRes:512, memMax:512]

    jdkVersion = 'JDK-17.0.2'
    rancherStackName = 'api'
    patternRule = /develop|master|hml/
}