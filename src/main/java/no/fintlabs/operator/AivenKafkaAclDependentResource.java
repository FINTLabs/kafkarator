package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.DesiredEqualsMatcher;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AivenProperties;
import no.fintlabs.FlaisExternalDependentResource;
import no.fintlabs.model.KafkaAclEntry;
import no.fintlabs.model.KafkaUser;
import no.fintlabs.service.AivenService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AivenKafkaAclDependentResource extends FlaisExternalDependentResource<KafkaUserAndAcl, AivenKafkaAclCrd, AivenKafkaAclSpec>
        implements Updater<KafkaUserAndAcl, AivenKafkaAclCrd> {

    private final AivenService aivenService;
    private final AivenProperties aivenProperties;

    public AivenKafkaAclDependentResource(AivenKafkaAclWorkflow workflow, AivenService aivenService, AivenProperties aivenProperties) {
        super(KafkaUserAndAcl.class, workflow);
        this.aivenService = aivenService;
        this.aivenProperties = aivenProperties;
        setPollingPeriod(Duration.ofMinutes(10).toMillis());
    }

    @Override
    protected KafkaUserAndAcl desired(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {

        return KafkaUserAndAcl.builder()
                .user(KafkaUser.fromUsername(primary.getMetadata().getName()))
                .aclEntries(
                        primary
                                .getSpec()
                                .getAcls()
                                .stream()
                                .map(acl -> acl.toAclEntry(primary.getMetadata().getName()))
                                .collect(Collectors.toList())
                )
                .build();
    }


    @Override
    public void delete(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String serviceName = aivenProperties.getService();
        String username = primary.getMetadata().getName();

        Optional<KafkaUserAndAcl> secondaryResource = context.getSecondaryResource(KafkaUserAndAcl.class);

        secondaryResource.ifPresent(kafkaUserAndAcl -> {
            kafkaUserAndAcl.getAclEntries().forEach(kafkaAclEntry -> {
                aivenService.deleteAclEntryForService(kafkaAclEntry.getId());
                log.debug("Deleted acl {} for user {} and topic {}", kafkaAclEntry.getId(), username, kafkaAclEntry.getTopic());

            });
            aivenService.deleteUserForService(kafkaUserAndAcl.getUser().getUsername());
            log.debug("Deleted user {} for service {}", username, serviceName);
        });
    }

    @Override
    public KafkaUserAndAcl create(KafkaUserAndAcl desired, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String serviceName = aivenProperties.getService();

        KafkaUser kafkaUser = aivenService.createUserForService(desired.getUser().getUsername());
        log.debug("Created user {} for service {}", desired.getUser().getUsername(), serviceName);

        List<KafkaAclEntry> kafkaAclEntries = desired.getAclEntries()
                .stream()
                .map(kafkaAclEntry -> {
                    KafkaAclEntry aclEntryForTopic = aivenService.createAclEntryForTopic(kafkaAclEntry);
                    log.debug("Created ACL for user {} on topic {}", kafkaAclEntry.getUsername(), kafkaAclEntry.getTopic());
                    return aclEntryForTopic;

                })
                .toList();

        return KafkaUserAndAcl.builder()
                .aclEntries(kafkaAclEntries)
                .user(kafkaUser)
                .build();

    }


    @Override
    public Set<KafkaUserAndAcl> fetchResources(AivenKafkaAclCrd primaryResource) {

        return aivenService
                .getUserAndAcl(primaryResource.getMetadata().getName())
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
    }

    @Override
    public ReconcileResult<KafkaUserAndAcl> reconcile(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        return super.reconcile(primary, context);
    }

    @Override
    protected KafkaUserAndAcl handleCreate(KafkaUserAndAcl desired, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        return super.handleCreate(desired, primary, context);
    }

    @Override
    public KafkaUserAndAcl update(KafkaUserAndAcl actual, KafkaUserAndAcl desired, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        log.debug("Updating Kafka acls");
        return aivenService.updateAclEntries(actual, desired);
    }

    @Override
    public Matcher.Result<KafkaUserAndAcl> match(KafkaUserAndAcl actualResource, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        DesiredEqualsMatcher<KafkaUserAndAcl, AivenKafkaAclCrd> matcher = new DesiredEqualsMatcher<>(this);

        return matcher.match(actualResource, primary, context);
    }
}
