package ma.locavite.reservation.service;

import ma.locavite.catalog.domain.Category;
import ma.locavite.reservation.config.PricingProperties;
import ma.locavite.reservation.domain.Discount;
import ma.locavite.reservation.domain.DiscountType;
import ma.locavite.reservation.domain.OptionPrice;
import ma.locavite.reservation.dto.*;
import ma.locavite.reservation.repository.DiscountRepository;
import ma.locavite.reservation.repository.OptionPriceRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class PricingAdminService {

    private static final BigDecimal DEFAULT_SIMULATION_CAUTION = new BigDecimal("3000.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SeasonService seasonService;
    private final OptionPriceRepository optionPriceRepository;
    private final DiscountRepository discountRepository;
    private final VehicleRepository vehicleRepository;
    private final PricingService pricingService;
    private final PricingProperties pricingProperties;

    public PricingAdminService(
            SeasonService seasonService,
            OptionPriceRepository optionPriceRepository,
            DiscountRepository discountRepository,
            VehicleRepository vehicleRepository,
            PricingService pricingService,
            PricingProperties pricingProperties) {
        this.seasonService = seasonService;
        this.optionPriceRepository = optionPriceRepository;
        this.discountRepository = discountRepository;
        this.vehicleRepository = vehicleRepository;
        this.pricingService = pricingService;
        this.pricingProperties = pricingProperties;
    }

    // ==================== SEASONS ====================

    @Transactional(readOnly = true)
    public List<SeasonResponse> getAllSeasons() {
        return seasonService.getAllResponses();
    }

    @Transactional
    public SeasonResponse createSeason(SeasonRequest request) {
        return seasonService.create(request);
    }

    @Transactional
    public SeasonResponse updateSeason(Long id, SeasonRequest request) {
        return seasonService.update(id, request);
    }

    @Transactional
    public void deleteSeason(Long id) {
        seasonService.delete(id);
    }

    // ==================== OPTIONS ====================

    @Transactional(readOnly = true)
    public List<OptionPriceResponse> getAllOptions() {
        return optionPriceRepository.findAll().stream()
                .map(OptionPriceResponse::fromEntity)
                .toList();
    }

    @Transactional
    public OptionPriceResponse createOption(OptionPriceRequest request) {
        optionPriceRepository.findByType(request.type()).ifPresent(existing -> {
            throw new IllegalArgumentException("Une option de type " + request.type() + " existe déjà");
        });

        OptionPrice option = new OptionPrice();
        option.setType(request.type());
        option.setLibelle(request.libelle().trim());
        option.setPrixJour(request.prixJour().setScale(2, RoundingMode.HALF_UP));
        option.setActif(request.actif() != null ? request.actif() : Boolean.TRUE);

        return OptionPriceResponse.fromEntity(optionPriceRepository.save(option));
    }

    @Transactional
    public OptionPriceResponse updateOption(Long id, OptionPriceRequest request) {
        OptionPrice existing = optionPriceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Option introuvable avec l'identifiant : " + id));

        optionPriceRepository.findByType(request.type())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Une autre option de type " + request.type() + " existe déjà");
                });

        existing.setType(request.type());
        existing.setLibelle(request.libelle().trim());
        existing.setPrixJour(request.prixJour().setScale(2, RoundingMode.HALF_UP));
        if (request.actif() != null) {
            existing.setActif(request.actif());
        }

        return OptionPriceResponse.fromEntity(optionPriceRepository.save(existing));
    }

    @Transactional
    public void deleteOption(Long id) {
        OptionPrice existing = optionPriceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Option introuvable avec l'identifiant : " + id));
        optionPriceRepository.delete(existing);
    }

    // ==================== DISCOUNTS ====================

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        return discountRepository.findAll().stream()
                .map(DiscountResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DiscountResponse createDiscount(DiscountRequest request) {
        validateDiscountRequest(request, null);

        Discount discount = new Discount();
        applyDiscountFields(discount, request);
        return DiscountResponse.fromEntity(discountRepository.save(discount));
    }

    @Transactional
    public DiscountResponse updateDiscount(Long id, DiscountRequest request) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Remise introuvable avec l'identifiant : " + id));
        validateDiscountRequest(request, id);

        applyDiscountFields(existing, request);
        return DiscountResponse.fromEntity(discountRepository.save(existing));
    }

    @Transactional
    public void deleteDiscount(Long id) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Remise introuvable avec l'identifiant : " + id));
        discountRepository.delete(existing);
    }

    // ==================== SIMULATION ====================

    @Transactional(readOnly = true)
    public PricingSimulateResponse simulatePrice(PricingSimulateRequest request) {
        Vehicle vehicle = resolveVehicleForSimulation(request);
        PriceQuoteResponse baseQuote = pricingService.calculate(
                vehicle,
                request.dateDebut(),
                request.dateFin(),
                request.options()
        );

        BigDecimal remisePromo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String appliedPromoCode = null;

        if (request.promoCode() != null && !request.promoCode().isBlank()) {
            String normalizedCode = request.promoCode().trim().toUpperCase(Locale.ROOT);
            Discount promo = discountRepository.findByCodeIgnoreCase(normalizedCode)
                    .orElseThrow(() -> new IllegalArgumentException("Code promo introuvable : " + normalizedCode));

            if (promo.getType() != DiscountType.PROMO_CODE || !promo.isValidOn(request.dateDebut())) {
                throw new IllegalArgumentException("Code promo expiré ou inactif : " + normalizedCode);
            }

            BigDecimal remainingSubtotal = baseQuote.sousTotal().subtract(baseQuote.remise()).max(BigDecimal.ZERO);
            BigDecimal computedPromo = baseQuote.sousTotal()
                    .multiply(promo.getPourcentage())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            remisePromo = computedPromo.min(remainingSubtotal).setScale(2, RoundingMode.HALF_UP);
            appliedPromoCode = promo.getCode();
        }

        BigDecimal remiseTotale = baseQuote.remise().add(remisePromo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = baseQuote.sousTotal().subtract(remiseTotale).add(baseQuote.options()).max(BigDecimal.ZERO);
        BigDecimal tva = taxable
                .multiply(pricingProperties.requireVatRate())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(tva).add(baseQuote.caution()).setScale(2, RoundingMode.HALF_UP);

        return new PricingSimulateResponse(
                baseQuote.sousTotal(),
                baseQuote.options(),
                baseQuote.remise(),
                remisePromo,
                remiseTotale,
                baseQuote.caution(),
                tva,
                total,
                baseQuote.jours(),
                appliedPromoCode
        );
    }

    private Vehicle resolveVehicleForSimulation(PricingSimulateRequest request) {
        if (request.vehicleId() != null) {
            return vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));
        }
        if (request.prixJour() == null) {
            throw new IllegalArgumentException("Veuillez fournir soit un vehicleId, soit un prixJour pour la simulation");
        }

        Category category = new Category();
        category.setCaution(request.caution() != null ? request.caution() : DEFAULT_SIMULATION_CAUTION);

        Vehicle synthetic = new Vehicle();
        synthetic.setPrixJour(request.prixJour());
        synthetic.setAgenceId(request.agenceId());
        synthetic.setCategory(category);
        return synthetic;
    }

    private void validateDiscountRequest(DiscountRequest request, Long excludeId) {
        if (request.dateDebut() != null && request.dateFin() != null
                && request.dateDebut().isAfter(request.dateFin())) {
            throw new IllegalArgumentException("La date de début de la remise doit être antérieure ou égale à la date de fin");
        }

        if (request.type() == DiscountType.LONG_DURATION) {
            if (request.minDays() == null || request.minDays() < 1) {
                throw new IllegalArgumentException("Le nombre minimal de jours est obligatoire pour une remise longue durée");
            }
        } else if (request.type() == DiscountType.PROMO_CODE) {
            if (request.code() == null || request.code().isBlank()) {
                throw new IllegalArgumentException("Le code promo est obligatoire pour une remise de type PROMO_CODE");
            }
            String normalizedCode = request.code().trim().toUpperCase(Locale.ROOT);
            boolean duplicate = excludeId == null
                    ? discountRepository.existsByCodeIgnoreCase(normalizedCode)
                    : discountRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, excludeId);
            if (duplicate) {
                throw new IllegalArgumentException("Le code promo '" + normalizedCode + "' existe déjà");
            }
        }
    }

    private void applyDiscountFields(Discount discount, DiscountRequest request) {
        discount.setType(request.type());
        discount.setCode(request.type() == DiscountType.PROMO_CODE && request.code() != null
                ? request.code().trim().toUpperCase(Locale.ROOT)
                : null);
        discount.setLibelle(request.libelle().trim());
        discount.setMinDays(request.type() == DiscountType.LONG_DURATION ? request.minDays() : null);
        discount.setPourcentage(request.pourcentage().setScale(2, RoundingMode.HALF_UP));
        discount.setDateDebut(request.dateDebut());
        discount.setDateFin(request.dateFin());
        discount.setActif(request.actif() != null ? request.actif() : Boolean.TRUE);
    }
}
