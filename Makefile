deploy: check-env
	git checkout dev
	git checkout master
	git merge dev --no-edit
	git tag -a $(TAG)
	git push && git push --tags
	git checkout dev

redeploy: check-env
	git checkout dev
	git checkout master
	git merge dev --no-edit
	git tag -d $(TAG)
	git push origin :refs/tags/$(TAG)
	git tag -a $(TAG)
	git push && git push --tags
	git checkout dev

check-env:
ifndef TAG
	$(error TAG is undefined)
endif
