package aurora.plugin.esb.adapter;

import org.apache.camel.builder.RouteBuilder;

import aurora.plugin.esb.AuroraEsbContext;
import aurora.plugin.esb.model.Producer;

public interface ProducerAdapter {

	public RouteBuilder createProducerBuilder(AuroraEsbContext esbContext,
			Producer producer);

	String getType();
}