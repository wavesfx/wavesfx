package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.ByteString;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.TransactionWithProofs;
import com.wavesplatform.wavesj.transactions.LeaseCancelTransactionV2;
import com.wavesplatform.wavesj.transactions.LeaseTransactionV2;
import com.wavesplatform.wavesj.transactions.TransferTransaction;
import com.wavesplatform.wavesj.transactions.TransferTransactionV2;

public class TxBroadcastGenerator {
    private final TransactionWithProofs<? extends Transaction> transaction;
    private final String mainToken;
    private final String node;

    public TxBroadcastGenerator(TransactionWithProofs<? extends Transaction> transaction, String mainToken, String node) {
        this.transaction = transaction;
        this.mainToken = mainToken;
        this.node = node;
    }

    private String nullString(String string) {
        if (string == null || string.equals(mainToken) || string.isEmpty()) {
            return "null";
        } else {
            return "'" + string + "'";
        }
    }

    private String generateBaseJson() {
        return  "        id: '" + transaction.getId() + "',\n" +
                "        type: " + transaction.getType() + ",\n" +
                "        version: " + transaction.getVersion() + ",\n" +
                "        fee: " + transaction.getFee() + ",\n" +
                "        senderPublicKey: '" + new ByteString(transaction.getSenderPublicKey().getPublicKey()) + "',\n" +
                "        timestamp: " + transaction.getTimestamp() + ",\n" +
                "        fee: " + transaction.getFee() + ",\n" +
                "        proofs: [\n" +
                "            '" + transaction.getProofs().get(0) + "'\n" +
                "        ]" + ",\n";
    }

    private String generateJson() {
        if (transaction instanceof TransferTransaction){
            final var tx = (TransferTransactionV2) transaction;
            return generateBaseJson() +
                    "        recipient: '" + tx.getRecipient() + "',\n" +
                    "        attachment: '" + tx.getAttachment().getBase58String() + "',\n" +
                    "        feeAssetId: null,\n" +
                    "        assetId: " + nullString(tx.getAssetId()) + ",\n" +
                    "        amount: " + tx.getAmount();
        } else if (transaction instanceof LeaseTransactionV2) {
            final var tx = (LeaseTransactionV2) transaction;
            return generateBaseJson() +
                    "        recipient: '" + tx.getRecipient() + "',\n" +
                    "        amount: " + tx.getAmount();
        } else if (transaction instanceof LeaseCancelTransactionV2) {
            final var tx = (LeaseCancelTransactionV2) transaction;
            return generateBaseJson() +
                    "        leaseId: '" + tx.getLeaseId() + "',\n" +
                    "        chainId: " + tx.getChainId();
        } else return "";
    }

    public String getHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Broadcast Transaction</title>\n" +
                "</head>\n" +
                "<script>\n" +
                "    function broadcastTransaction(){\n" +
                "        var xhr = new XMLHttpRequest();\n" +
                "        var url = \""+ node + "/transactions/broadcast\";\n" +
                "        xhr.open(\"POST\", url, true);\n" +
                "        xhr.setRequestHeader(\"Content-Type\", \"application/json\");\n" +
                "        xhr.onreadystatechange = function () {\n" +
                "            if (xhr.readyState === 4) {\n" +
                "                var json = JSON.parse(xhr.responseText);\n" +
                "                console.log(xhr.responseText);\n" +
                "                document.getElementById(\"result\").innerText = JSON.stringify(json)\n" +
                "            }\n" +
                "        };\n" +
                "        xhr.send(data)\n" +
                "    }\n" +
                "    var data = JSON.stringify({\n" +
                generateJson() + "\n" +
                "    }, undefined, 2);\n" +
                "    window.onload = function() {\n" +
                "        document.getElementById(\"txInfo\").innerHTML = data;\n" +
                "    }\n" +
                "</script>\n" +
                "<body>\n" +
                "    <div>\n" +
                "        <h3>Transaction Info:</h3>\n" +
                "        <pre>Note: attachments are Base58 encoded</pre>\n" +
                "        <pre id=\"txInfo\"></pre>\n" +
                "    </div>\n" +
                "    <button onclick=\"broadcastTransaction()\">Broadcast Transaction</button>\n" +
                "    <pre id=\"result\"></pre>\n" +
                "</body>\n" +
                "</html>";
    }
}
