
# Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
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

Param (
    [string] $cmd = 'create'
)

Set-StrictMode -Version Latest

$versionMinimum = [Version]'7.1.0'
$versionCurrent = $PSVersionTable.PSVersion
if ($versionMinimum -gt $versionCurrent) {
    throw "PowerShell $versionMinimum statt $versionCurrent erforderlich"
}

$namespace = 'acme'

function New-Namespace {
    kubectl create namespace $namespace
    kubectl config set-context --current --namespace $namespace
}

function Remove-Namespace {
    kubectl config set-context --current --namespace default
    kubectl delete namespace $namespace
}

switch ($cmd) {
    'create' { New-Namespace; break }
    'delete' { Remove-Namespace; break }

    default { Write-Output "$script [create|delete]" }
}
