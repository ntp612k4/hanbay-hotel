package com.tpanh.hotel.booking.dto.request;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Do not silently truncate fractional currency values or coerce strings into money. */
public class CheckOutAmountDeserializer extends ValueDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Long.class, "Số tiền phải là số nguyên đồng");
        }
        return parser.getLongValue();
    }
}
