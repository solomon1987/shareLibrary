def labels = "slave-${UUID.randomUUID().toString()}"

// 引用共享库
@Library("jenkins_shareLibrary")

// 应用共享库中的方法
def tools = new org.devops.tools()
def sendEmail = new org.devops.sendEmail()
def build = new org.devops.build()

// 前端传来的变量
def gitBranch = env.branch
def gitUrl = env.git_url
def buildShell = env.build_shell
def image = env.image
def devops_cd_git = env.devops_cd_git



pipeline {
    agent {
    kubernetes {
        label labels
      yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    some-label: some-label-value
spec:
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
  containers:
  - name: jnlp
    image: registry.cn-hangzhou.aliyuncs.com/rookieops/inbound-agent:4.3-4
  - name: golang
    image: golang:1.16.5
    command:
    - cat
    tty: true
  - name: docker
    image: docker:latest
    command:
    - cat
    tty: true
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
  - name: kustomize
    image: registry.cn-hangzhou.aliyuncs.com/rookieops/kustomize:v3.8.1
    command:
    - cat
    tty: true
"""
    }
  }

    environment{
        auth = 'joker'
    }

    options {
        timestamps()    // 日志会有时间
        skipDefaultCheckout()   // 删除隐式checkout scm语句
        disableConcurrentBuilds()   //禁止并行
        timeout(time:1,unit:'HOURS') //设置流水线超时时间
    }


    stages {
        // 拉取代码
        stage('GetCode') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: "${gitBranch}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: 'github_pull_ssh', url: "${gitUrl}"]]])
                }
            }

        // 单元测试和编译打包
        stage('Build&Test') {
            steps {
                container('golang') {
                    script{
                        sh """
                        go env -w GOPROXY=https://goproxy.cn
                        CGO_ENABLED=0 go build -o ./wioms ./main.go
                        [ -d deploy  ] || mkdir deploy
                        cp -rf templates static configs wioms deploy
                        """
                    }
                }
            }
        }

        // 构建镜像
        stage('BuildImage') {
            steps {
                container('docker') {
                    script{
                        sh """
                        IMAGE_NAME="liyubao1232000/wioms:v$BUILD_NUMBER"
                        docker login -u liyubao1232000 -p QAZxsw123456
                        docker build -t $IMAGE_NAME .
                        docker push $IMAGE_NAME
                        docker rmi $IMAGE_NAME
                        """
                    }
                }
            }
        }
        // 部署
        stage('Deploy') {
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding',
                credentialsId: 'github_pull_ssh',
                usernameVariable: 'DEVOPS_USER',
                passwordVariable: 'DEVOPS_PASSWORD']]){
                    container('kustomize') {
                        script{
                            APP_DIR="${JOB_NAME}".split("_")[0]
                            sh """
                            git remote set-url origin http://${DEVOPS_USER}:${DEVOPS_PASSWORD}@${devops_cd_git}
                            git config --global user.name "Administrator"
                            git config --global user.email "coolops@163.com"
                            git clone http://${DEVOPS_USER}:${DEVOPS_PASSWORD}@${devops_cd_git} /opt/devops-cd
                            cd /opt/devops-cd
                            git pull
                            cd /opt/devops-cd/${APP_DIR}
                            kustomize edit set image ${image}:${imageTag}
                            git commit -am 'image update'
                            git push origin master
                            """
                        }
                    }
                }
            }
        }
        // 接口测试
        stage('InterfaceTest') {
            steps{
                sh 'echo "接口测试"'
            }
        }
    }
    // 构建后的操作
    post {
        success {
            script{
                println("success:只有构建成功才会执行")
                currentBuild.description += "\n构建成功！"
                // deploy.AnsibleDeploy("${deployHosts}","-m ping")
                sendEmail.SendEmail("构建成功",toEmailUser)
                // dingmes.SendDingTalk("构建成功 ✅")
            }
        }
        failure {
            script{
                println("failure:只有构建失败才会执行")
                currentBuild.description += "\n构建失败!"
                sendEmail.SendEmail("构建失败",toEmailUser)
                // dingmes.SendDingTalk("构建失败 ❌")
            }
        }
        aborted {
            script{
                println("aborted:只有取消构建才会执行")
                currentBuild.description += "\n构建取消!"
                sendEmail.SendEmail("取消构建",toEmailUser)
                // dingmes.SendDingTalk("构建失败 ❌","暂停或中断")
            }
        }
    }
}
