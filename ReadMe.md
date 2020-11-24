# Hinweise zum Programmierbeispiel

<Juergen.Zimmermann@HS-Karlsruhe.de>

> Diese Datei ist in Markdown geschrieben und kann z.B. mit IntelliJ IDEA
> gelesen werden. Näheres zu Markdown gibt es in einem
> [Wiki](http://bit.ly/Markdown-Cheatsheet)

## Powershell

Überprüfung, ob sich Powershell-Skripte (s.u.) starten lassen:

```PowerShell
    Get-ExecutionPolicy -list
```

`CurrentUser` muss das Recht `RemoteSigned` haben. Ggf. muss das
Ausführungsrecht gesetzt werden:

```PowerShell
    Set-ExecutionPolicy RemoteSigned CurrentUser
```

## Falls die Speichereinstellung für Gradle zu großzügig ist

In `gradle.properties` bei `org.gradle.jvmargs` den voreingestellten Wert
(2 GB) ggf. reduzieren.

## Übersetzung und lokale Ausführung

### Start und Stop des Servers in der Kommandozeile

In einer Powershell wird der Server mit dem Profil `dev` gestartet:

```PowerShell
    .\gradlew bootRun
```

Mit `<Strg>C` kann man den Server herunterfahren, weil in der Datei
`application.yml` im Verzeichnis `src\main\resources` _graceful shutdown_
konfiguriert ist.

#### Port

Normalerweise nimmt man beim Entwickeln für HTTP den Port `8080` und für HTTPS
den Port `8443`. Damit das Entwickeln mit Kubernetes nicht zu komplex wird,
wird dort auf TLS bzw. HTTPS verzichtet und "nur" HTTP genutzt. Deshalb wird
in diesem und in den späteren Beispielen der Port 8080 genutzt - auch für HTTPS.

#### _HTTP Client_ von IntelliJ IDEA

Im Verzeichnis `restclient` gibt es Dateien mit der Endung `.rest`, in denen
HTTP-Requests vordefiniert sind. Diese kann man mit verschiedenen Umgebungen
("environment") ausführen.

#### Evtl. Probleme mit dem Kotlin Daemon

Falls der _Kotlin Daemon_ beim Übersetzen nicht mehr reagiert, sollte man
alte Dateien im Verzeichnis `%LOCALAPPDATA%\kotlin\daemon` löschen.

#### Evtl. Kontinuierliches Monitoring von Dateiänderungen

Kontinuierliches Monitoring durch die _DevTools_ von _Spring Boot_ ist
kann für einen lokal laufenden Server genutzt werden und ist z.B. bei diesen
URLs beschrieben:

- https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using-boot-devtools
- https://www.vojtechruzicka.com/spring-boot-devtools

## Docker-Image, Docker-Container und Dashboard von Docker Desktop

### Docker-Daemon

Vorbemerkung: Die Kommunikation mit dem _Docker-Daemon_, d.h. _Dienst_ bei
Windows, sollte mit der Benutzerkennung erfolgen, mit der man _Docker Desktop_
installiert hat, weil diese Benutzerkennung bei der Installation zur
Windows-Gruppe `docker-users` hinzugefügt wurde.

### Dockerfile und Docker-Image

Durch die (Konfigurations-) Datei `Dockerfile` kann man ein Docker-Image
erstellen. Diese Datei kann man noch vielfältig optimieren, was aber später
durch _Cloud Native Buildpack_ (s.u.) obsolet wird.

Zuerst erstellt man ein JAR-Archiv mit dem Microservice und baut anschließend
das Docker-Image:

```CMD
    gradle bootJar
    docker build --tag kunde:1.0
```

Nachdem das Docker-Image erstellt ist, kann man es im _Docker Dashboard_ sehen.
Im _System Tray_ (rechts unten in der _Taskleiste_) ist das Docker-Icon
(_Whale_). Über das Kontextmenü (rechte Maustaste) vom Whale-Icon kann man das
_Dashboard_ aufrufen und dann wird im Menüpunkt _Images_ das Image angezeigt.

Nun kann man einen Docker-Container mit dem Imagestarten, indem
`.\microservice.ps1 container` aufgeruft, wodurch das komplexe Kommando
`docker run` gekapselt ist. Den gestarteten Docker-Container kann man ebenfalls
im Docker Dashboard sehen.

Nun läuft der Microservice als Docker-Container mit HTTPS und kann über den
_HTTP Client_ von IntelliJ IDEA aufgerufen werden. Dabei wird auch der
Container-interne Port 8080 des Microservice "kunde" als Port 8080 für
localhost freigegeben.

Über das Docker Dashboard wird der Container beendet, und dabei wird der
Microservice mit "graceful shutdown" heruntergefahren.

### Docker-Image und Cloud Native Buildpack

Mit folgendem Kommando kann man durch Spring Boot unter Verwendung von _Cloud
Native Buildpack_ (cnb) ein optimiertes und geschichtetes Docker-Image _ohne_
`Dockerfile` erstellen:

```CMD
    .\gradlew bootBuildImage
```

Alternativ kann man mit dem Gradle-Plugin _Jib_ von Google ein Docker-Image
erstellen, das für Java allgemein, aber nicht für Spring optimiert ist:

```CMD
    .\gradlew jibDockerBuild
```

_Spring Boot_ verwendet als Tag-Name die Version des Gradle-Projekts.
Mit dem folgenden Kommando kann man dann einen Docker-Container mit dem Image
`kunde` starten und im Docker Dashboard sehen:

```CMD
    .\microservice.ps1 container
```

## Kubernetes durch Docker Desktop

### Rechnername in der Datei `hosts`

Wenn man mit Kubernetes arbeitet, bedeutet das auch, dass man i.a. über TCP
kommuniziert. Deshalb sollte man überprüfen, ob in der Datei
`C:\Windows\System32\drivers\etc\hosts` der eigene Rechnername mit seiner
IP-Adresse eingetragen ist. Zum Editieren dieser Datei sind Administrator-Rechte
notwendig.

### `kubectl`

Das wichtigste Kommando, um mit Kubernetes zu kommunizieren, ist `kubectl`, wozu
es etliche Unterkommandos gibt, wie z.B. `kubectl apply`, `kubectl get` oder
`kubectl describe`.

Durch das PowerShell-Skript `kunde.ps1` sind die Aufrufe von `kubectl` gekapselt
und dadurch vereinfacht.

### Namespace

In Kubernetes gibt es Namespaces ("Namensräume") wie in

- Betriebssystemen durch Verzeichnisse, z.B. in Windows oder Unix
- Programmiersprachen, z.B. durch `package` in Kotlin und Java
- Datenbanksystemen, z.B. in Oracle und PostgreSQL.

Genauso wie in Datenbanksystemen gibt es in Kubernetes _keine_ untergeordneten
Namespaces. Vor allem ist es in Kubernetes empfehlenswert für die eigene
Software einen _eigenen_ Namespace anzulegen und **NICHT** den Default-Namespace
zu benutzen.

Ein neuer Namespace, z.B. `acme`, wird durch das Kommando
`kubectl create namespace acme` angelegt.

### Installation

`.\kunde.ps1 install` baut zunächst ein Image für Docker. Danach wird eine
_Configmap_ für Kubernetes erstellt, in der Umgebungsvariable zur Konfiguration
des Deployments bereitgestellt werden. Abschließend wird in Kubernetes sowohl
ein _Deployment_ mit dieser _Configmap_ als auch ein _Service_ für ggf. externen
Zugriff erstellt. Die dazu notwendige Manifest- bzw. Konfigurationsdatei ist
`kube\kunde.yml`.

Externer Zugriff ist beim Entwickeln relativ einfach durch _Port-Forwarding_
möglich, indem das Skript `.\kunde.ps1 forward` aufgerufen wird. Dann kann man
z.B. mit restclient von IntelliJ oder in einer 2. Powershell mit
`curl --silent --user admin:p http://localhost:8080/api/00000000-0000-0000-0000-000000000001`
auf den in Kubernetes laufenden Microservice zugreifen.

### Deinstallieren

Mit `.\kunde.ps1 uninstall` werden die installierten _Service_, _Deployment_ und
_Configmap_ wieder aus Kubernetes entfernt, wobei implizit auch das _Pod_
entfernt wird.

### Aktualisieren des Deployments

Wenn man den eigenen Code für den Microservice modifiziert und dann ein neues
Docker-Image baut, dann kann man das neu gebaute Docker-Image bei den Images im
_Docker Dashboard_ sehen.

Was dabei aber i.a. nicht geändert wurde ist die Manifest-Datei (hier:
`kube\kunde.yml`). Ein Deployment wäre aus technischer Sicht zwar erfolgreich,
weil es keine Fehlermeldung gibt, aber die Statusmeldung `unchanged` wäre nicht
aus Entwicklersicht sicher nicht das gewünschte Resultat. Deshalb muss zunächst
das Deployment entfernt werden, damit das alte, nicht mehr benötigte
Docker-Image gelöscht werden kann. Nun kann ein neues Docker-Image gebaut und
in einem Deployment an Kubernetes übergeben werden. Alle diese Schritte werden
ausgeführt, wenn man das Skript `.\kunde.ps1 redeploy` aufruft.

Falls das nicht mehr benötigte Docker-Image wegen "Race Conditions" nicht
gelöscht wurde, kann man sich mit `docker image ls` sämtliche Images auflisten
lassen und mit `docker image rm ...` die Images mit einem Tag `<none>` löschen,
indem man ihre IDs verwendet.

### Octant vs. Kubernetes Dashboard vs. Kubernetes-Extension für VS Code

#### Kubernetes Dashboard

_Kubernetes Dashboard_ <https://github.com/kubernetes/dashboard> ist vor allem
für typische Administrationsaufgaben geeignet.

#### Kubernets-Extension für VS-Code

Für _VS Code_ gibt es die Extension _Kubernetes_ von Microsoft. Diese
Extension ist ähnlich wie _Octant_ auf die Bedürfnisse der Entwickler/innen
zugeschnitten und ermöglicht den einfachen Zugriff auf ein Terminal oder
die Logs.

#### Octant

_Octant_ ist von VMware und vor allem für Entwickler/innen geeignet. Octant muss
von einer PowerShell aus gestartet werden, in dem man im Verzeichnis
`C:\Zimmermann\octant` das Kommando `.\octant.exe` aufruft.

Zunächst stellt man rechts oben, den Namespace von `default` auf `acme`. Danach
hat man unter _Namespace Overview_ eine Übersicht über z.B. _Pods_,
_Deployments_, _Stateful Sets_, _Services_, _Config Maps_ und _Secrets_.

In der Navigationsleiste am linken Rand findet gezielt man bei

- Workloads: Pods, Deployments, Stateful Sets
- Discovery and Loadbalancing: Services
- Config and Storage: Config Maps und Secrets

Bei einem ausgewählten Pod hat man direkten Zugriff auf

- die Logging-Ausgaben in der Konsole der virtualisierten Software
- ein Terminal mit einer Shell durch `bash`, falls das zugrundeliegende
  Docker-Image die bash enthält, wie z.B. bei MongoDB, wo man dann im Laufe des
  Semesters DB-Queries mit dem Kommandozeilen-Client absetzen kann.
  Das Docker-Image für die eigenen Microservices enthält keine bash.

## Codeanalyse durch ktlint und detekt

```CMD
    .\gradlew ktlint detekt
```

## API-Dokumentation

```CMD
    .\gradlew dokka
```
