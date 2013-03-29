/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.journal.AbstractJournal;
import org.apache.jackrabbit.core.journal.AppendRecord;
import org.apache.jackrabbit.core.journal.InstanceRevision;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.JournalFactory;
import org.apache.jackrabbit.core.journal.MemoryRevision;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordIterator;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.impl.InitializationProcessorImpl;
import org.hippoecm.repository.impl.ReferenceWorkspaceImpl;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.repository.modules.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import static org.apache.jackrabbit.core.config.RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE;

public class LocalHippoRepository extends HippoRepositoryImpl {


    /** System property for overriding the repository path */
    public static final String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_CONFIG_PROPERTY = "repo.config";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_UPGRADE_PROPERTY = "repo.upgrade";

    /** System property for overriding the servlet config file */
    public static final String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public static final String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    /** The advised threshold on the number of modified nodes to hold in transient session state */
    public static int batchThreshold = 96;

    protected static final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);


    /** hippo decorated root session */
    private HippoSession rootSession;

    private LocalRepositoryImpl jackrabbitRepository = null;

    /** Whether to generate a dump.xml file of the /hippo:configuration node at shutdown */
    private final boolean dump = false;

    /** Whether to perform an automatic upgrade from previous releases */
    private UpgradeFlag upgradeFlag = UpgradeFlag.TRUE;

    /** Whether to reindex after upgrading */
    private boolean upgradeReindexFlag = false;

    /** Whether to run a derived properties validation after upgrading */
    private boolean upgradeValidateFlag = true;

    private String repoPath;

    /** When during startup a situation is detected that a restart is required, this flag signals this, but only one restart should be appropriate */
    boolean needsRestart = false;

    private static enum UpgradeFlag {
        TRUE, FALSE, ABORT
    }

    private ModuleManager moduleManager;

    protected LocalHippoRepository() throws RepositoryException {
        super();
    }

    protected LocalHippoRepository(String location) throws RepositoryException {
        super(location);
    }

    public static HippoRepository create(String location) throws RepositoryException {
        LocalHippoRepository localHippoRepository;
        if (location == null) {
            localHippoRepository = new LocalHippoRepository();
        } else {
            localHippoRepository = new LocalHippoRepository(location);
        }
        localHippoRepository.initialize();
        VMHippoRepository.register(location, localHippoRepository);
        return localHippoRepository;
    }

    @Override
    public String getLocation() {
        return super.getLocation();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Construct the repository path, default getWorkingDirectory() is used.
     * If the system property repo.path can be used to override the default.
     * If repo.path starts with a '.' then the path is taken relative to the
     * getWorkingDirectory().
     * @return The absolute path to the file repository
     */
    private String getRepositoryPath() {
        if (repoPath != null) {
            return repoPath;
        }

        final String pathProp = System.getProperty(SYSTEM_PATH_PROPERTY);

        if (pathProp == null || pathProp.isEmpty()) {
            repoPath = getWorkingDirectory();
        } else if (pathProp.charAt(0) == '.') {
            // relative path
            repoPath = getWorkingDirectory() + System.getProperty("file.separator") + pathProp;
        } else {
            repoPath = RepoUtils.stripFileProtocol(pathProp);
        }

        log.info("Using repository path: " + repoPath);
        return repoPath;
    }

    /**
     * If the "file://" protocol is used, the path MUST be absolute.
     * In all other cases the config file is used as a class resource.
     * @return InputStream to the repository config
     * @throws RepositoryException
     */
    private static InputStream getRepositoryConfigAsStream() throws RepositoryException {
        String configPath = System.getProperty(SYSTEM_CONFIG_PROPERTY);

        if (configPath == null || "".equals(configPath)) {
            configPath = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        if (configPath == null || "".equals(configPath)) {
            log.info("Using default repository config: " + DEFAULT_REPOSITORY_CONFIG);
            return LocalHippoRepository.class.getResourceAsStream(DEFAULT_REPOSITORY_CONFIG);
        }

        if (!configPath.startsWith("file:")) {
            log.info("Using resource repository config: " + configPath);
            return LocalHippoRepository.class.getResourceAsStream(configPath);
        }

        configPath = RepoUtils.stripFileProtocol(configPath);

        log.info("Using file repository config: file:/" + configPath);

        File configFile = new File(configPath);
        try {
            return new BufferedInputStream(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            throw new RepositoryException("Repository config not found: file:/" + configPath);
        }
    }

    private class LocalRepositoryImpl extends RepositoryImpl {
        LocalRepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
            super(repConfig);
        }
        @Override
        public Session getRootSession(String workspaceName) throws RepositoryException {
            return super.getRootSession(workspaceName);
        }
        void enableVirtualLayer(boolean enabled) throws RepositoryException {
            isStarted = enabled;
        }

        protected FileSystem getFileSystem() {
            return super.getFileSystem();
        }

        private boolean isClustered() {
            return getRepositoryConfig().getClusterConfig() != null;
        }
    }

    protected void initialize() throws RepositoryException {
        initializeStartup();
        if(needsRestart) {
            log.warn("restarting repository after upgrade cycle");
            close();
            if (upgradeReindexFlag) {
                log.warn("post migration cycle forced reindexing");
                initializeReindex();
            }
            initializeStartup();
            ((HippoSecurityManager) jackrabbitRepository.getSecurityManager()).configure();
            if (upgradeValidateFlag) {
                log.warn("post migration cycle validating content");
                ((org.hippoecm.repository.impl.SessionDecorator)rootSession).postValidation();
            }
        } else {
            ((HippoSecurityManager) jackrabbitRepository.getSecurityManager()).configure();
        }
    }

    private void initializeReindex() {
        final File basedir = new File(getRepositoryPath());
        try {
            FileUtils.deleteDirectory(new File(basedir, "repository/index"));
            FileUtils.deleteDirectory(new File(basedir, "workspaces/default/index"));
        } catch (IOException e) {
            log.warn("Unable to delete index", e);
        }
    }

    private void initializeStartup() throws RepositoryException {

        Modules.setModules(new Modules(Thread.currentThread().getContextClassLoader()));

        RepositoryConfig config = readRepositoryConfig();

        jackrabbitRepository = new LocalRepositoryImpl(config);
        repository = jackrabbitRepository;

        String result = System.getProperty(SYSTEM_UPGRADE_PROPERTY);
        if (result != null) {
            for(String option : result.split(",")) {
                String key = "", value = "";
                if (option.contains("=")) {
                    String[] keyValue = option.split("=");
                    key = keyValue[0];
                    value = keyValue[1];
                }
                if (option.equalsIgnoreCase("abort")) {
                    upgradeFlag = UpgradeFlag.ABORT;
                } else if(key.equalsIgnoreCase("batchsize")) {
                   LocalHippoRepository.batchThreshold  = Integer.parseInt(value);
                } else if(option.equalsIgnoreCase("reindex")) {
                    upgradeReindexFlag = true;
                } else if(option.equalsIgnoreCase("validate")) {
                    upgradeValidateFlag = true;
                } else if(option.equalsIgnoreCase("skipreindex")) {
                    upgradeReindexFlag = false;
                } else if(option.equalsIgnoreCase("skipvalidate")) {
                    upgradeValidateFlag = false;
                } else if(option.equalsIgnoreCase("true")) {
                    upgradeFlag = UpgradeFlag.TRUE;
                } else if(option.equalsIgnoreCase("false")) {
                    upgradeFlag = UpgradeFlag.FALSE;
                } else {
                    log.warn("Unrecognized upgrade option \""+option+"\"");
                }
            }
        }
        switch(upgradeFlag) {
        case FALSE:
            log.info("Automatic upgrade enabled: false");
            break;
        case TRUE:
            log.info("Automatic upgrade enabled: true (reindexing "+(upgradeReindexFlag?"on":"off")+" revalidation "+(upgradeValidateFlag?"on":"off")+")");
            break;
        case ABORT:
            log.info("Automatic upgrade enabled: abort on upgrade required");
        }

        repository = new DecoratorFactoryImpl().getRepositoryDecorator(repository);

        try {
            // get the current root/system session for the default workspace for namespace and nodetypes init
            Session jcrRootSession =  jackrabbitRepository.getRootSession(null);

            if(!jcrRootSession.getRootNode().isNodeType("mix:referenceable")) {
                jcrRootSession.getRootNode().addMixin("mix:referenceable");
                jcrRootSession.save();
            }

            boolean hasHippoNamespace;
            try {
                jcrRootSession.getNamespaceURI("hippo");
                hasHippoNamespace = true;
            } catch (NamespaceException ex) {
                hasHippoNamespace = false;
            }

            if (hasHippoNamespace) {
                switch(upgradeFlag) {
                case TRUE:
                    jackrabbitRepository.enableVirtualLayer(false);
                    migrate(jcrRootSession);
                    if (needsRestart) {
                        return;
                    }
                    break;
                case FALSE:
                    break;
                case ABORT:
                    throw new RepositoryException("ABORT");
                }
            }

            Session syncSession = jcrRootSession.impersonate(new SimpleCredentials("system", new char[] {}));

            final InitializationProcessorImpl initializationProcessor = new InitializationProcessorImpl();

            initializeSystemNodeTypes(syncSession, jackrabbitRepository.getFileSystem(), initializationProcessor);

            jackrabbitRepository.enableVirtualLayer(true);

            // After initializing namespaces and nodetypes switch to the decorated session.
            rootSession = DecoratorFactoryImpl.getSessionDecorator(syncSession.impersonate(new SimpleCredentials("system", new char[]{})));

            if (!rootSession.getRootNode().hasNode("hippo:configuration")) {
                log.info("Initializing configuration content");
                InputStream configuration = getClass().getResourceAsStream("configuration.xml");
                if (configuration != null) {
                    initializationProcessor.initializeNodecontent(rootSession, "/", configuration, getClass().getPackage().getName() + ".configuration.xml");
                } else {
                    log.error("Could not initialize configuration content: ResourceAsStream not found: configuration.xml");
                }
                rootSession.save();
            } else {
                log.info("Initial configuration content already present");
            }

            // load all extension resources
            try {
                initializationProcessor.loadExtensions(rootSession);
            } catch (IOException ex) {
                throw new RepositoryException("Could not obtain initial configuration from classpath", ex);
            }
            initializationProcessor.processInitializeItems(rootSession);
            if (log.isDebugEnabled()) {
                initializationProcessor.dryRun(rootSession);
            }

            if (!hasHippoNamespace) {
                migrate(jcrRootSession);
            }

            moduleManager = new ModuleManager(jcrRootSession.impersonate(new SimpleCredentials("system", new char[]{})));
            moduleManager.start();

            syncSession.logout();

        } catch (LoginException ex) {
            log.error("no access to repository by repository itself", ex);
        }
    }

    private RepositoryConfig readRepositoryConfig() throws RepositoryException {
        Properties variables = new Properties(System.getProperties());
        variables.setProperty(REPOSITORY_HOME_VARIABLE, getRepositoryPath());
        RepositoryConfigurationParser parser =
                new ClusterConfigAddingConfigurationParser(variables);

        RepositoryConfig config = parser.parseRepositoryConfig(new InputSource(getRepositoryConfigAsStream()));
        config.init();
        return config;
    }

    /**
     * Migration via the UpdaterEngine is enabled when the UpdaterEngine is on the classpath.
     * Users must explicitly add the dependency to their project in order to use the old
     * style updaters.
     */
    private void migrate(final Session jcrRootSession) throws RepositoryException {
        try {
            final Class<?> updaterEngineClass = Class.forName("org.hippoecm.repository.updater.UpdaterEngine");
            final Method migrate = updaterEngineClass.getMethod("migrate", Session.class, boolean.class);
            final Session migrateSession = DecoratorFactoryImpl.getSessionDecorator(
                    jcrRootSession.impersonate(new SimpleCredentials("system", new char[]{})));
            needsRestart = (Boolean) migrate.invoke(null, migrateSession, jackrabbitRepository.isClustered());
            migrateSession.logout();
        } catch (ClassNotFoundException ignore) {
            log.debug("UpdaterEngine not found");
        } catch (NoSuchMethodException e) {
            log.error("Unexpected error while trying to invoke UpdaterEngine", e);
        } catch (InvocationTargetException e) {
            log.error("Unexpected error while trying to invoke UpdaterEngine", e);
        } catch (IllegalAccessException e) {
            log.error("Unexpected error while trying to invoke UpdaterEngine", e);
        }
    }

    private void initializeSystemNodeTypes(final Session syncSession, final FileSystem fileSystem, final InitializationProcessorImpl initializationProcessor) throws RepositoryException {
        final Properties checksumProperties = new Properties();
        try {
            if (fileSystem.exists("/cnd-checksums")) {
                InputStream in = null;
                try {
                    in = fileSystem.getInputStream("/cnd-checksums");
                    checksumProperties.load(in);
                } catch (IOException e) {
                    log.error("Failed to read cnd checksum file. All system cnds will be reloaded.", e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        } catch (FileSystemException e) {
            log.error("Failed to read cnd checksum from the file system. All system cnds will be reloaded", e);
        }
        for(String cndName : new String[] { "hippo.cnd", "hipposys.cnd", "hipposysedit.cnd", "hippofacnav.cnd", "hipposched.cnd" }) {
            InputStream cndStream = null;
            try {
                cndStream = getClass().getClassLoader().getResourceAsStream(cndName);
                final String checksum = getChecksum(cndStream);
                cndStream.close();
                if (!checksum.equals(checksumProperties.getProperty(cndName))) {
                    log.info("Initializing nodetypes from: " + cndName);
                    cndStream = getClass().getClassLoader().getResourceAsStream(cndName);
                    initializationProcessor.initializeNodetypes(syncSession.getWorkspace(), cndStream, cndName);
                    syncSession.save();
                    checksumProperties.setProperty(cndName, checksum);
                } else {
                    log.info("No need to reload " + cndName + ": no changes");
                }
            } catch (ConstraintViolationException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (InvalidItemStateException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (ItemExistsException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (LockException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (NoSuchNodeTypeException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (ParseException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (VersionException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (NoSuchAlgorithmException e) {
                throw new RepositoryException("Could not initialize repository with hippo node types", e);
            } catch (IOException e) {
                throw new RepositoryException("Could not initialize repository with hippo node types", e);
            } finally {
                if (cndStream != null) { try { cndStream.close(); } catch (IOException ignore) {} }
            }
        }

        OutputStream out = null;
        try {
            out = fileSystem.getOutputStream("/cnd-checksums");
            checksumProperties.store(out, null);
        } catch (IOException e) {
            log.error("Failed to store cnd checksum file.", e);
        } catch (FileSystemException e) {
            log.error("Failed to store cnd checksum file.", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private String getChecksum(final InputStream cndStream) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md5 = MessageDigest.getInstance("SHA-256");
        final byte[] buffer = new byte[1024];
        int read;
        do {
            read = cndStream.read(buffer, 0, buffer.length);
            if (read > 0) {
                md5.update(buffer, 0, read);
            }
        } while (read > 0);

        final byte[] bytes = md5.digest();
        //convert the byte to hex format
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    @Override
    public synchronized void close() {

        if (moduleManager != null) {
            moduleManager.stop();
            moduleManager = null;
        }

        Session session = null;
        if (dump && repository != null) {
            try {
                session = login();
                java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
                session.exportSystemView("/hippo:configuration", out, false, false);
            } catch (IOException ex) {
                log.error("Error while dumping configuration: " + ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                log.error("Error while dumping configuration: " + ex.getMessage(), ex);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        // stop all listeners on rootSession
        if (rootSession != null && rootSession.isLive()) {
            try {
                ObservationManager obMgr = rootSession.getWorkspace().getObservationManager();
                EventListenerIterator elIter = obMgr.getRegisteredEventListeners();
                while (elIter.hasNext()) {
                    EventListener el = elIter.nextEventListener();
                    log.debug("Removing EventListener from root session");
                    obMgr.removeEventListener(el);
                }
            } catch (Exception ex) {
                log.error("Error while removing listener: " + ex.getMessage(), ex);
            }
        }

        if (jackrabbitRepository != null) {
            try {
                jackrabbitRepository.shutdown();
                jackrabbitRepository = null;
            } catch (Exception ex) {
                log.error("Error while shuting down jackrabbitRepository: " + ex.getMessage(), ex);
            }
        }
        repository = null;

        super.close();
    }

    @Override
    public InitializationProcessor getInitializationProcessor() {
        return new InitializationProcessorImpl();
    }

    @Override
    public ReferenceWorkspace getOrCreateReferenceWorkspace() throws RepositoryException {
        return new ReferenceWorkspaceImpl(jackrabbitRepository);
    }

    /**
     * We need a cluster configuration to trigger the reshuffling of documents under a handle.
     */
    private static class ClusterConfigAddingConfigurationParser extends RepositoryConfigurationParser {
        public ClusterConfigAddingConfigurationParser(final Properties variables) {
            super(variables, new ConnectionFactory());
        }

        @Override
        protected ClusterConfig parseClusterConfig(final Element parent, final File home) throws ConfigurationException {
            ClusterConfig config = super.parseClusterConfig(parent, home);
            if (config == null) {
                config = new ClusterConfig(UUID.randomUUID().toString(), 0, new JournalFactory() {
                    @Override
                    public Journal getJournal(final NamespaceResolver resolver) throws RepositoryException {
                        return new DementedJournal();
                    }
                });
            }
            return config;
        }
    }

    private static class DementedJournal extends AbstractJournal {

        private final InstanceRevision revision = new MemoryRevision();

        @Override
        protected void doLock() throws JournalException {
        }

        @Override
        protected void append(final AppendRecord record, final InputStream in, final int length) throws JournalException {
        }

        @Override
        protected void doUnlock(final boolean successful) {
        }

        @Override
        public void close() {
        }

        @Override
        public InstanceRevision getInstanceRevision() throws JournalException {
            return revision;
        }

        @Override
        public RecordIterator getRecords(final long startRevision) throws JournalException {
            return new EmptyRecordIterator();
        }

        @Override
        public RecordIterator getRecords() throws JournalException {
            return null;
        }
    }

    private static class EmptyRecordIterator implements RecordIterator {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Record nextRecord() throws NoSuchElementException, JournalException {
            throw new NoSuchElementException();
        }

        @Override
        public void close() {
        }
    }

}
