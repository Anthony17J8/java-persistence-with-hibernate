package com.ico.ltd.querying.domain;

import java.util.Date;

public class ItemSummaryFactory {

    private ItemSummaryFactory() {
    }

    public static ItemSummary newItemSummary(Long itemId,
                                             String name,
                                             Date auctionEnd) {
        return new ItemSummary(itemId, name, auctionEnd);

    }
}
