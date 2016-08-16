deploy: check-env
	git checkout master
	git merge dev
	git push
	git checkout release
	git merge master
	git tag -s $(TAG)
	git push && git push --tags

check-env:
ifndef TAG
	$(error TAG is undefined)
endif
