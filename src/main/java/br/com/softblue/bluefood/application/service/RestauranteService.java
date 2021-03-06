package br.com.softblue.bluefood.application.service;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import br.com.softblue.bluefood.domain.cliente.Cliente;
import br.com.softblue.bluefood.domain.cliente.ClienteRepository;
import br.com.softblue.bluefood.domain.restaurante.ItemCardapio;
import br.com.softblue.bluefood.domain.restaurante.ItemCardapioRepository;
import br.com.softblue.bluefood.domain.restaurante.Restaurante;
import br.com.softblue.bluefood.domain.restaurante.RestauranteComparator;
import br.com.softblue.bluefood.domain.restaurante.RestauranteRepository;
import br.com.softblue.bluefood.domain.restaurante.SearchFilter;
import br.com.softblue.bluefood.domain.restaurante.SearchFilter.SearchType;
import br.com.softblue.bluefood.util.SecurityUtils;

@Service
public class RestauranteService {
	
	@Autowired//automatiza a criacao do repository
	private RestauranteRepository restauranteRepository;
	
	@Autowired
	private ClienteRepository clienteRepository;
	
	@Autowired
	private ImageService imageService;
	
	@Autowired
	private ItemCardapioRepository itemCardapioRepository;
	
	@Transactional
	public void saveRestaurante(Restaurante restaurante) throws ValidationException {
		if(!validateEmail(restaurante.getEmail(), restaurante.getId())) {
			throw new ValidationException("Este e-mail j� est� cadastrado no sistema!");
			
		}
		if(restaurante.getId() != null) {//alterando
			Restaurante restauranteDB = restauranteRepository.findById(restaurante.getId()).orElseThrow();
			restaurante.setSenha(restauranteDB.getSenha());
			restaurante.setLogotipo(restauranteDB.getLogotipo());
			restauranteRepository.save(restaurante);
		}else {//craindo
			restaurante.encryptPassword();
			restauranteRepository.save(restaurante);
			restaurante.setLogotipoFileName();
			imageService.uploadLogotipo(restaurante.getLogotipoFile(), restaurante.getLogotipo());
			
		}
		
		
		
		
	}
	
	private boolean validateEmail(String email, Integer id) {
		
		Cliente cliente = clienteRepository.findByEmail(email);
		Restaurante restaurante = restauranteRepository.findByEmail(email);
		
		if(cliente !=null) {
			return false;
		}
		
		if (restaurante != null) {
			if (id == null) {
				return false;
			}
			
			if(!restaurante.getId().equals(id)) {
				return false;
			}
		}
		
		return true;
	}
	
	public List<Restaurante> search(SearchFilter filter){
		List<Restaurante> restaurantes;
		if (filter.getSearchType() == SearchType.Texto) {
			restaurantes = restauranteRepository.findByNomeIgnoreCaseContaining(filter.getTexto());
		}
		else if (filter.getSearchType() == SearchType.Categoria) {
			restaurantes =  restauranteRepository.findByCategorias_Id(filter.getCategoriaId());
			
		}else {
			throw new IllegalStateException("O tipo de busca "+ filter.getSearchType() + " n�o � suportado");
		}
		
		Iterator<Restaurante> it= restaurantes.iterator();//iterator aponta para objeto especifico da cole��o, so foi utilizado por poder remover
		while(it.hasNext()) {
			Restaurante restaurante = it.next();
			double taxaEntrega = restaurante.getTaxaEntrega().doubleValue();
			if(filter.isEntregaGratis() && taxaEntrega > 0 || filter.isEntregaGratis() && taxaEntrega == 0) {
				it.remove();
			}
			
		}
		RestauranteComparator comparator = new RestauranteComparator(filter, SecurityUtils.loggedCliente().getCep());
		restaurantes.sort(comparator);//usa o comparator como base para ordenar a lista de restaurantes
		return restaurantes;
		
	}
	@Transactional
	public void saveItemItemCardapio(ItemCardapio itemCardapio) {
		itemCardapio = itemCardapioRepository.save(itemCardapio);	
		itemCardapio.setImagemFileName();
		imageService.uploadComida(itemCardapio.getImagemFile(), itemCardapio.getImagem());
	}

}
