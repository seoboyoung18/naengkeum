package com.fridgefamer.dto.response.wishlist;

/**
 * 찜 등록/해제 응답 — API 명세 §6.
 *
 * <pre>{ "wishlisted": true }  // 등록 시 true, 해제 시 false</pre>
 */
public record WishlistToggleResponse(
        boolean wishlisted
) {}