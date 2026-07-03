package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse indiquant si le portefeuille peut effectuer des opérations.
 */
@Schema(description = "Capacité opérationnelle du portefeuille")
public record WalletCanOperateResponse(
                @Schema(description = "true si le portefeuille peut effectuer des opérations", example = "true") boolean canOperate) {
}
