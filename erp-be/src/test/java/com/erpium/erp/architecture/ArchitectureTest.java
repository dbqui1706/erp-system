package com.erpium.erp.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architectural compliance test suite enforcing layered boundaries, DI standards,
 * modular monolith slice isolation, and generalized inter-module encapsulation.
 */
@AnalyzeClasses(packages = "com.erpium.erp", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    /**
     * Prohibits field injection with @Autowired. Enforces constructor injection.
     */
    @ArchTest
    public static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("Field injection with @Autowired is prohibited; use constructor injection instead.")
            .allowEmptyShould(true);

    /**
     * Prevents Controllers from bypassing the Service layer and invoking Repositories directly.
     */
    @ArchTest
    public static final ArchRule controllers_must_not_depend_on_repositories = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .because("Controllers must not call Repositories directly; call Services instead.")
            .allowEmptyShould(true);

    /**
     * Ensures Repositories remain independent and do not depend on upper layers.
     */
    @ArchTest
    public static final ArchRule repositories_must_not_depend_on_services_or_controllers = noClasses()
            .that().areAnnotatedWith(Repository.class)
            .should().dependOnClassesThat().areAnnotatedWith(Service.class)
            .orShould().dependOnClassesThat().areAnnotatedWith(RestController.class)
            .because("Repositories must not depend on Services or Controllers.")
            .allowEmptyShould(true);

    /**
     * Enforces Modular Monolith Slice Isolation:
     * Prevents circular dependencies between domain packages under com.erpium.erp.*.
     */
    @ArchTest
    public static final ArchRule slices_must_be_free_of_cycles = slices()
            .matching("com.erpium.erp.(*)..")
            .should().beFreeOfCycles()
            .because("Domain modules must be free of cyclic dependencies to maintain modular monolith maintainability.")
            .allowEmptyShould(true);

    /**
     * Enforces Inter-Module Port Isolation:
     * Product module must access Organization exclusively via public ports (e.g. OrganizationValidatorPort)
     * and must never access Organization internal entities or repositories.
     */
    @ArchTest
    public static final ArchRule product_module_must_not_access_organization_internals = noClasses()
            .that().resideInAPackage("..product..")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.organization.Organization")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.organization.OrganizationRepository")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.organization.OrganizationStatusHistory")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.organization.OrganizationStatusHistoryRepository")
            .because("Product module must access Organization exclusively via OrganizationValidatorPort or public DTOs.")
            .allowEmptyShould(true);

    /**
     * Enforces Upstream/Downstream Dependency Direction:
     * Organization is a foundational core module and must not depend downstream on Product module.
     */
    @ArchTest
    public static final ArchRule organization_module_must_not_depend_on_product_module = noClasses()
            .that().resideInAPackage("..organization..")
            .should().dependOnClassesThat().resideInAPackage("..product..")
            .because("Organization module is foundational and must not depend downstream on Product module.")
            .allowEmptyShould(true);

    /**
     * Enforces Order Module Encapsulation:
     * Order module must interact with Organization and Product exclusively via public services,
     * ports, and DTOs, and never access internal entities or repositories directly.
     */
    @ArchTest
    public static final ArchRule order_module_must_not_access_other_modules_internal_entities = noClasses()
            .that().resideInAPackage("..order..")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.organization.Organization")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.organization.OrganizationRepository")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.product.Product")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.product.ProductRepository")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.product.ProductPriceTier")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.erpium.erp.product.ProductPriceTierRepository")
            .because("Order module must interact with Organization and Product strictly via public services, ports, and DTOs.")
            .allowEmptyShould(true);
}
