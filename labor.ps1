# Copyright (C) 2017 -  Juergen Zimmermann, Hochschule Karlsruhe
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Nicht als Shellscript, da Kubernetes innerhalb von Docker Desktop nur als Windows-Installation vorliegt
# https://docs.microsoft.com/en-us/powershell/scripting/developer/cmdlet/approved-verbs-for-windows-powershell-commands?view=powershell-7

# Aufruf:   .\labor.ps1 install-k8s|uninstall-k8s|container|image-dockerfile

# "Param" muss in der 1. Zeile sein
Param (
    [string]$cmd = 'help'
)

Set-StrictMode -Version Latest

$versionMinimum = [Version]'7.1.0'
$versionCurrent = $PSVersionTable.PSVersion
if ($versionMinimum -gt $versionCurrent) {
    throw "PowerShell $versionMinimum statt $versionCurrent erforderlich"
}

# Titel setzen
$script = $myInvocation.MyCommand.Name
$host.ui.RawUI.WindowTitle = $script

$microservice = 'labor'
$imageVersion = '1.0'
$port = '8080'
$namespace = 'acme'

function Install-K8s {
    kubectl apply --filename k8s\$microservice`.yaml
    Write-Output ''
    Write-Output 'Beispiel-Aufruf mit curl:'
    Write-Output "    curl --silent --user admin:p http://localhost:${port}/api/00000000-0000-0000-0000-000000000001"
    Write-Output "    curl --silent --user admin:p http://localhost:${port}/home"
    Write-Output ''

    $configmap = "$microservice-env-dev"
    Write-Output "Umgebungsvariable in der Configmap ${configmap}:"
    Write-Output "    kubectl describe configmap $configmap --namespace $namespace"
    Write-Output "    kubectl get configmap $configmap --output jsonpath='{.data}' --namespace $namespace"
    Write-Output ''
}

function Uninstall-K8s {
    kubectl config set-context --current --namespace $namespace

    kubectl delete deployment,service $microservice
    docker image rm ${microservice}:$imageVersion
    kubectl delete configmap $microservice-env-dev
}

function Invoke-Container {
    $image = $microservice
    $imageTag = '1.0'
    $containerName = $image
    $containerHostname = $image

    Write-Output "Port-Binding: port=$port, containerPort=$port"
    Write-Output 'Logfile: /var/log/spring/application.log'
    Write-Output 'Mounting: /var/log/spring ist zugreifbar im Unterverzeichnis build\log'
    Write-Output ''

    docker run --publish ${port}:$port `
        --env TZ=Europe/Berlin `
        --env spring.profiles.active=dev `
        --env logging.file.name=/var/log/spring/application.log `
        --mount type=bind,source=$PWD\build\log,destination=/var/log/spring `
        --name $containerName --hostname $containerHostname --rm ${image}:$imageTag
}

function New-Image-Dockerfile {
    Write-Output ''
    Write-Output 'VORHER NICHT VERGESSEN:   .\gradlew bootJar'
    Write-Output ''

    docker build --tag ${microservice}:1.0 .
}

# https://docs.microsoft.com/en-us/powershell/scripting/developer/cmdlet/approved-verbs-for-windows-powershell-commands?view=powershell-7#invoke-vs-start
switch ($cmd) {
    'install-k8s' { Install-K8s; break }
    'uninstall-k8s' { Uninstall-K8s; break }

    'container' { Invoke-Container; break }
    'image-dockerfile' { New-Image-Dockerfile; break }

    default { Write-Output "$script install-k8s|uninstall-k8s|container|image-dockerfile" }
}
