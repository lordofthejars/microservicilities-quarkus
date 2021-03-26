package org.acme;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public class RatingServiceFallback implements FallbackHandler<Rate> {

    @Override
    public Rate handle(ExecutionContext context) {
        Rate rate = new Rate();
        rate.rate = 0;
        return rate;
    }
    
}
