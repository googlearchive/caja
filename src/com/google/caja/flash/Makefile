# TODO(felix8a): this should be absorbed by build.xml
# TODO(felix8a): tests should be moved to tests tree
# TODO(felix8a): swfobject.js should be in third_party and added
# to build process.

MXMLC = mxmlc
MXMLC_OPT = \
	--source-path=../../../.. \
	--static-link-runtime-shared-libraries \
	--debug=true \
	--show-actionscript-warnings=true \
	$(eol)

ALL = \
	flashbridge.swf \
	flashbridgetest.swf \
	tests/BridgeAware.swf \
	tests/BridgeOblivious.swf \
	tests/flashbridge.swf \
	$(eol)

AS_DEPS = FlashBridgeClient.as events/*.as external/*.as net/*.as

.PHONY: all
all: $(ALL)
	@true

flashbridge.swf: FlashBridge.as $(AS_DEPS)
	$(MXMLC) $(MXMLC_OPT) -o $@ FlashBridge.as

flashbridgetest.swf: FlashBridgeTest.as $(AS_DEPS)
	$(MXMLC) $(MXMLC_OPT) -o $@ FlashBridgeTest.as

tests/BridgeAware.swf: tests/BridgeAware.mxml $(AS_DEPS)
	$(MXMLC) $(MXMLC_OPT) -o $@ tests/BridgeAware.mxml

tests/BridgeOblivious.swf: tests/BridgeOblivious.mxml $(AS_DEPS)
	$(MXMLC) $(MXMLC_OPT) -o $@ tests/BridgeOblivious.mxml

tests/flashbridge.swf: flashbridge.swf
	cp flashbridge.swf tests

DEST1 = dest1-not-specified:no-path
DEST2 = dest2-not-specified:no-path

.PHONY: push
push: all
	rsync -aCiL --delete --delete-excluded tests/ $(DEST1)
	rsync -aCiL --delete --delete-excluded tests/ $(DEST2)

.PHONY: clean
clean:
	rm -f $(ALL)
