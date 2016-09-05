deploy: check-env
	git checkout master
	git merge dev --no-edit
	git push
	git checkout release
	git merge master --no-edit
	git tag -s $(TAG)
	git push && git push --tags
	git checkout dev

check-env:
ifndef TAG
	$(error TAG is undefined)
endif
