pipeline 
{
    agent 
    { 
        label 'AndroidStudio && JDK && BuildTools2022'
    } 
    triggers 
    {
        pollSCM 'H/15 * * * *'
    }
    options
    {
        buildDiscarder(logRotator(numToKeepStr: '3'))
    }
    tools
    {
        jdk "JDK17"
        msbuild "BuildTools 2022"
    }
    stages 
    {
        stage('Checkout') 
        {
            steps 
            {
                deleteDir()
                checkout([$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], gitTool: 'Default', userRemoteConfigs: [[url: 'git@github.com:moba-ag/ScrcpyForAndroid.git']]])
            }
        }
        stage('Compile')
        {
            steps
            {
                withCredentials([usernamePassword(credentialsId: 'moba-github-packages-token-readonly', passwordVariable: 'GITHUB_TOKEN', usernameVariable: 'GITHUB_USER')]) 
                {
                    bat """
                        call gradlew.bat build -x lint -x test
                    """
                }
                script
                {
                    def jFile = readJSON file: 'service/build/outputs/apk/release/output-metadata.json'
                    writeFile file: 'version.txt', text: jFile.elements[0].versionName
                }
            }
        }
/*
        stage('Create SBOM')
        {
            steps 
            {
                bat """
                    call gradlew.bat :service:cyclonedxBom --info
                """
            }
        }
*/
        stage('Changelog')
        {
            steps
            {
                convertMdToPdf input: 'changelog/org.client.scrcpy.changelog.md'
            }
        }
        stage('Archive')
        {
            steps
            {
                bat 'copy service\\build\\outputs\\apk\\release\\org.client.scrcpy.apk org.client.scrcpy.apk'
                bat 'copy service\\build\\reports\\sbom.json sbom.json'
                archiveArtifacts artifacts: 'org.client.scrcpy.apk, version.txt, changelog/org.client.scrcpy.changelog.pdf', followSymlinks: false
                mobaReleaseManagement deploymentDescriptor: 'deployment-descriptor.xml', versionInfoFile: "version.txt"
            }
        }
    }
}