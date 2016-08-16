deploy: check-env
	git push
	git checkout master
	git merge dev --log --no-edit
	git push
	git checkout release
	git merge master --log --no-edit
	git tag -s $(TAG)
	git push && git push --tags
	git checkout dev

check-env:
ifndef TAG
	$(error TAG is undefined)
endif
