echo "Deriving version using 'git describe'..."
git describe
export VERSION=$(git describe | sed 's/vfs-gcs-//g')
echo "Publishing version: ${VERSION}"

status=$(curl -s --head -w %{http_code} -o /dev/null https://dl.bintray.com/dalet-oss/maven/com/dalet/celarli/commons/vfs-gcs/${VERSION}/)
if [ $status -eq 200 ]
then
  echo "Version already published - nothing to do here"
else
  mvn deploy -DperformRelease=true -Drevision=${VERSION}
fi
