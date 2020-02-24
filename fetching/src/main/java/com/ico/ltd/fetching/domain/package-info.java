@org.hibernate.annotations.FetchProfiles({

        // Each profile has a name, this is a simple string we have isolated in a constant.
        @FetchProfile(name = Item.PROFILE_JOIN_SELLER,
                fetchOverrides = @FetchProfile.FetchOverride(
                        entity = Item.class,
                        association = "seller",
                        mode = FetchMode.JOIN
                )),

        @FetchProfile(name = Item.PROFILE_JOIN_BIDS,
                fetchOverrides = @FetchProfile.FetchOverride(
                        entity = Item.class,
                        association = "bids",
                        mode = FetchMode.JOIN
                ))
})
package com.ico.ltd.fetching.domain;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;