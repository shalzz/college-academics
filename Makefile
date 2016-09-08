deploy: check-env
	git checkout dev
	git push
	git checkout master
	git merge dev --no-edit
	git tag -s $(TAG)
	git push && git push --tags
	git checkout dev

check-env:
ifndef TAG
	$(error TAG is undefined)
endif
