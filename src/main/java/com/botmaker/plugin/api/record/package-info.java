/**
 * Recording input as a bot's own calls.
 *
 * <p>The host records the mouse and keyboard and recognises {@link com.botmaker.plugin.api.record.Gesture}s. A
 * plugin says which of its methods writes each gesture with {@link com.botmaker.plugin.api.record.Records} on
 * the method, and answers the parameter types only it understands with a
 * {@link com.botmaker.plugin.api.record.RecordedValue}. No Java text crosses: the host writes the call.
 */
package com.botmaker.plugin.api.record;
