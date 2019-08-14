package esa.s1pdgs.cpoc.ingestion.product;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.ingestion.obs.ObsAdapter;
import esa.s1pdgs.cpoc.mqi.model.queue.IngestionDto;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;

public class AuxiliaryProductFactory implements ProductFactory<ProductDto> {	
    /**
     * Pattern for configuration files to extract data
     */

	@Override
	public List<Product<ProductDto>> newProducts(final File file, final IngestionDto ingestionDto, final ObsAdapter obsAdapter) throws ProductException {			
		final List<Product<ProductDto>> result = new ArrayList<>();
		result.add(newProduct(file, obsAdapter));
		
		// find manifest
		if (file.isDirectory()) {
	        try {
				final List<Path> manifesto = Files.walk(Paths.get(file.getPath()), FileVisitOption.FOLLOW_LINKS)
					.filter(p -> p.getFileName().toString().toLowerCase().equals("manifest.safe"))
					.collect(Collectors.toList());
				
				for (final Path path : manifesto) {
					final File mnfstFile = path.toFile();
					result.add(newProduct(mnfstFile, obsAdapter));
				}
			} catch (IOException e) {
				throw new ProductException(
						String.format("Error traversing product %s: %s", file, e.getMessage()),
						e
				);
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
		return "AuxiliaryProductFactory";
	}

	private final Product<ProductDto> newProduct(final File file, final ObsAdapter obsAdapter) {
		final Product<ProductDto> prod = new Product<>();
		prod.setFamily(ProductFamily.AUXILIARY_FILE);
		prod.setFile(file);	
		
		final ProductDto dto = new ProductDto(
				file.getName(), 
				obsAdapter.toObsKey(file), 
				ProductFamily.AUXILIARY_FILE
		);
		prod.setDto(dto);
		return prod;
	}


}
