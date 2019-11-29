package com.wavesfx.wavesfx.gui;

public enum FXMLView {
    ADDRESS("/wallet/address.fxml"),
    ASSETS("/wallet/assets.fxml"),
    ASSET_INFO_DIALOG("/dialog/assetInfo.fxml"),
//    BLOCKHEADER_CELL("/wallet/blockHeaderCell.fxml"),
    BURN_ASSETS("/wallet/burnAssets.fxml"),
    BURN_TOKEN("/dialog/burnToken.fxml"),
    CONFIRM_SEED("/accountCreator/confirmSeed.fxml"),
    CONFIRM_BURN_ASSETS("/dialog/confirmBurnAssets.fxml"),
    CONFIRM_MOVE_ASSETS("/dialog/confirmMoveAssets.fxml"),
    CONFIRM_TRANSACTION("/dialog/confirmTransfer.fxml"),
    DASHBOARD("/wallet/dashboard.fxml"),
    GET_SEED("/accountCreator/getSeed.fxml"),
    IMPORT_ACCOUNT("/accountCreator/importAccount.fxml"),
    LEASING("/wallet/leasing.fxml"),
    LOGIN("/login/login.fxml"),
    LOGIN_INFO("/accountCreator/loginInfo.fxml"),
    MASS_TRANSFER("/wallet/massTransfer.fxml"),
    MOVE_ASSETS("/wallet/moveAssets.fxml"),
    NETWORK("/accountCreator/network.fxml"),
    REISSUE_TOKEN("/dialog/reissueToken.fxml"),
    SEND("/wallet/send.fxml"),
    SETTINGS("/wallet/settings.fxml"),
    SET_SCRIPT("/wallet/setScript.fxml"),
    TOKEN_ISSUE("/wallet/tokenIssue.fxml"),
    TOS("/dialog/tos.fxml"),
    TRANSACTIONS("/wallet/transactions.fxml"),
//    TRANSACTION_INFO("/dialog/transactionInfo.fxml"),
    TRANSFER("/wallet/transfer.fxml"),
    WALLET("/wallet/walletView.fxml");

    final private String path;

    FXMLView(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }

}
