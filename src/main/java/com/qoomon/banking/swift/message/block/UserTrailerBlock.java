package com.qoomon.banking.swift.message.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.qoomon.banking.swift.message.block.exception.BlockFieldParseException;
import com.qoomon.banking.swift.message.block.exception.BlockParseException;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <b>User Header Block</b>
 * <p>
 * <b>Sub Blocks</b>
 * <pre>
 * 1: MAC - Message Authentication Code calculated based on the entire contents of the message using a key that has been exchanged with the destination and a secret algorithm. Found on message categories 1,2,4,5,7,8, most 6s and 304.
 * 2: PAC - Proprietary Authentication Code.
 * 3: CHK - Checksum calculated for all message types.
 * 4: TNG - Training.
 * 5: PDE - Possible Duplicate Emission added if user thinks the same message was sent previously
 * 6: DLM - Added by SWIFT if an urgent message (U) has not been delivered within 15 minutes, or a normal message (N) within 100 minutes.
 * </pre>
 * <b>Example</b><br>
 * {MAC:12345678}{CHK:123456789ABC}
 *
 * @see <a href="https://www.ibm.com/support/knowledgecenter/SSBTEG_4.3.0/com.ibm.wbia_adapters.doc/doc/swift/swift72.htm">https://www.ibm.com/support/knowledgecenter/SSBTEG_4.3.0/com.ibm.wbia_adapters.doc/doc/swift/swift72.htm</a>
 */
public class UserTrailerBlock implements SwiftBlock {

    public static final String BLOCK_ID_5 = "5";

    public final Optional<String> messageAuthenticationCode;

    public final Optional<String> proprietaryAuthenticationCode;

    public final Optional<String> checksum;

    public final Optional<String> training;

    public final Optional<String> possibleDuplicateEmission;

    public final Optional<String> deliveryDelay;

    private final ImmutableMap<String, GeneralBlock> additionalSubblocks;


    public UserTrailerBlock(String messageAuthenticationCode, String proprietaryAuthenticationCode, String checksum, String training, String possibleDuplicateEmission, String deliveryDelay, Map<String, GeneralBlock> additionalSubblocks) {

        Preconditions.checkArgument(additionalSubblocks != null, "additionalSubblocks can't be null");

        this.messageAuthenticationCode = Optional.ofNullable(messageAuthenticationCode);
        this.proprietaryAuthenticationCode = Optional.ofNullable(proprietaryAuthenticationCode);
        this.checksum = Optional.ofNullable(checksum);
        this.training = Optional.ofNullable(training);
        this.possibleDuplicateEmission = Optional.ofNullable(possibleDuplicateEmission);
        this.deliveryDelay = Optional.ofNullable(deliveryDelay);
        this.additionalSubblocks = ImmutableMap.copyOf(additionalSubblocks);
    }

    public static UserTrailerBlock of(GeneralBlock block) throws BlockFieldParseException {
        Preconditions.checkArgument(block.getId().equals(BLOCK_ID_5), "unexpected block id 'v '", block.getId());

        SwiftBlockReader blockReader = new SwiftBlockReader(new StringReader(block.getContent()));

        String messageAuthenticationCode = null;
        String proprietaryAuthenticationCode = null;
        String checksum = null;
        String training = null;
        String possibleDuplicateEmission = null;
        String deliveryDelay = null;
        Map<String, GeneralBlock> additionalSubblocks = new HashMap<>();

        try {
            GeneralBlock subblock;
            while ((subblock = blockReader.readBlock()) != null) {
                switch (subblock.getId()) {
                    case "MAC":
                        messageAuthenticationCode = subblock.getContent(); // TODO regex check
                        break;
                    case "PAC":
                        proprietaryAuthenticationCode = subblock.getContent(); // TODO regex check
                        break;
                    case "CHK":
                        checksum = subblock.getContent(); // TODO regex check
                        break;
                    case "TNG":
                        training = subblock.getContent(); // TODO regex check
                        break;
                    case "PDE":
                        possibleDuplicateEmission = subblock.getContent(); // TODO regex check
                        break;
                    case "DLM":
                        deliveryDelay = subblock.getContent(); // TODO regex check
                        break;
                    default:
                        additionalSubblocks.put(subblock.getId(), subblock);
                        break;
                }
            }
        } catch (BlockParseException e) {
            throw new BlockFieldParseException("Block '" + block.getId() + "' content error", e);
        }

        return new UserTrailerBlock(messageAuthenticationCode, proprietaryAuthenticationCode, checksum, training, possibleDuplicateEmission, deliveryDelay, additionalSubblocks);
    }

    public Optional<String> getMessageAuthenticationCode() {
        return messageAuthenticationCode;
    }

    public Optional<String> getChecksum() {
        return checksum;
    }

    public Optional<String> getPossibleDuplicateEmission() {
        return possibleDuplicateEmission;
    }

    public Optional<String> getDeliveryDelay() {
        return deliveryDelay;
    }

    public GeneralBlock getAdditionalSubblock(String id) {
        return additionalSubblocks.get(id);
    }

    public Optional<String> getTraining() {
        return training;
    }

    public Optional<String> getProprietaryAuthenticationCode() {
        return proprietaryAuthenticationCode;
    }

    @Override
    public String getId() {
        return BLOCK_ID_5;
    }

    @Override
    public String getContent() {
        StringBuilder contentBuilder = new StringBuilder();
        if(messageAuthenticationCode.isPresent()) {
            contentBuilder.append(BlockUtils.swiftTextOf("MAC", messageAuthenticationCode.get()));
        }
        if(proprietaryAuthenticationCode.isPresent()) {
            contentBuilder.append(BlockUtils.swiftTextOf("PAC", proprietaryAuthenticationCode.get()));
        }
        if(checksum.isPresent()) {
            contentBuilder.append(BlockUtils.swiftTextOf("CHK", checksum.get()));
        }
        if(training.isPresent()) {
            contentBuilder.append(BlockUtils.swiftTextOf("TNG", training.get()));
        }
        if(possibleDuplicateEmission.isPresent()) {
            contentBuilder.append(BlockUtils.swiftTextOf("PDE", possibleDuplicateEmission.get()));
        }
        if(deliveryDelay.isPresent()) {
            contentBuilder.append(BlockUtils.swiftTextOf("DLM", deliveryDelay.get()));
        }

        for (GeneralBlock subblock : additionalSubblocks.values()) {
            contentBuilder.append(BlockUtils.swiftTextOf(subblock.getId(), subblock.getContent()));
        }
        return contentBuilder.toString();
    }
}
