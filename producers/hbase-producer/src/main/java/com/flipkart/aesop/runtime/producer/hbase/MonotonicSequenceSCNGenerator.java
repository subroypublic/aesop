/*
 * Copyright 2012-2015, the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flipkart.aesop.runtime.producer.hbase;

import com.flipkart.aesop.runtime.producer.spi.SCNGenerator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 * <code>MonotonicSequenceSCNGenerator</code> is an implementation of the {@link SCNGenerator} that handles mastership change
 * as monotonically increasing generation changes to create relay SCNs from local SCNs and host identifiers.
 * This SCN generator works on the following assumptions/approach:
 * <pre>
 * 	<li>The local SCN has the format of Timestamp in millis
 *  </li>
 *  <li>
 *      The generated relay SCN has the format : high 42 bits are local SCNs timestamp in millis, next 12 bits are unused
 *      bits which is futuristic in case timestamp changes the bit which is very unlikely in near future, next 10 bits
 *      are for running number. This approach can support 2^10 events in millisecond.
 *  </li>
 * <pre>
 * @author Milap Wadhwa
 * @version 1.2, 9 Jul 2015
 */
public class MonotonicSequenceSCNGenerator implements SCNGenerator {

    /**maintains previous SCN generated by SEP event (WalEdits event)*/
    private AtomicLong prevSEPSCN;
    /** maintains the sequence number for the last snc*/
    private AtomicInteger eventSequence;
    public MonotonicSequenceSCNGenerator() {
        prevSEPSCN = new AtomicLong(0);
        eventSequence = new AtomicInteger(0);
    }
    
    /**
     * Interface method implementation. get the Relayer SCN based on localSCN
     * @see com.flipkart.aesop.runtime.producer.spi.SCNGenerator#getSCN(long, java.lang.String)
     */
    @Override
    public long getSCN(long localSCN, String hostId) {
        return frameSCN(localSCN);
    }

    private long frameSCN(long localSCN) {
        /**localSCN -1 or 0 implies the initial start state of the relayer.Hence, returning as-is*/
        if (localSCN == -1 || localSCN == 0) {
            return localSCN;
        }
        long scn = localSCN;
        if (prevSEPSCN.get() != localSCN) {
            eventSequence.set(0);
            prevSEPSCN.set(localSCN);
        }
        scn <<= 10;
        scn |= eventSequence.incrementAndGet();
        return scn;
    }
}