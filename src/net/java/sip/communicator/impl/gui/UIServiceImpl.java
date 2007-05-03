/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.JFrame;

import net.java.sip.communicator.impl.gui.GuiActivator.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.AccountRegWizardContainerImpl;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPanel;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.PluginComponentEvent;
import net.java.sip.communicator.service.gui.event.PluginComponentListener;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

/**
 * An implementation of the <tt>UIService</tt> that gives access to other
 * bundles to this particular swing ui implementation.
 * 
 * @author Yana Stamcheva
 */
public class UIServiceImpl
    implements UIService
{
    private static final Logger logger = Logger.getLogger(UIServiceImpl.class);

    private PopupDialogImpl popupDialog;

    private AccountRegWizardContainerImpl wizardContainer;

    private Map registeredPlugins = new Hashtable();

    private Vector pluginComponentListeners = new Vector();

    private static final List supportedContainers = new ArrayList();
    static
    {
        supportedContainers.add(UIService.CONTAINER_MAIN_TOOL_BAR);
        supportedContainers.add(UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
        supportedContainers.add(UIService.CONTAINER_GROUP_RIGHT_BUTTON_MENU);
        supportedContainers.add(UIService.CONTAINER_TOOLS_MENU);
        supportedContainers.add(UIService.CONTAINER_CHAT_TOOL_BAR);
    }

    private static final Hashtable exportedWindows = new Hashtable();

    private MainFrame mainFrame;

    private LoginManager loginManager;
    
    private ContactListPanel contactListPanel;

    private ConfigurationFrame configurationFrame;

    private boolean exitOnClose = true;

    /**
     * Creates an instance of <tt>UIServiceImpl</tt>.
     * 
     * @param mainFrame The main application window.
     */
    public UIServiceImpl()
    {}
    
    /**
     * Initializes all frames and panels and shows the gui.
     */
    public void loadApplicationGui()
    {
        this.setDefaultThemePack();
        
        this.mainFrame = new MainFrame();
        
        this.loginManager = new LoginManager(mainFrame);
        
        this.contactListPanel = mainFrame.getContactListPanel();

        this.popupDialog = new PopupDialogImpl();

        this.wizardContainer = new AccountRegWizardContainerImpl(mainFrame);

        this.configurationFrame = new ConfigurationFrame(mainFrame);
        
        mainFrame.setContactList(GuiActivator.getMetaContactListService());
        
        if(ConfigurationManager.isApplicationVisible())
            SwingUtilities.invokeLater(new RunApplicationGui());
        
        SwingUtilities.invokeLater(new RunLoginGui());
        
        this.initExportedWindows();
    }

    /**
     * Implements addComponent in UIService interface. Stores a plugin component
     * and fires a PluginComponentEvent to inform all interested listeners that
     * a plugin component has been added.
     * 
     * @param containerID The <tt>ContainerID</tt> of the plugable container.
     * @param component The component to add.
     * 
     * @see UIService#addComponent(ContainerID, Object)
     */
    public void addComponent(ContainerID containerID, Object component)
        throws ClassCastException, IllegalArgumentException
    {
        if (!supportedContainers.contains(containerID))
        {
            throw new IllegalArgumentException(
                "The constraint that you specified is not"
                    + " supported by this UIService implementation.");
        }
        else if (!(component instanceof Component))
        {

            throw new ClassCastException(
                "The specified plugin is not a valid swing or awt component.");
        }
        else
        {
            if (registeredPlugins.containsKey(containerID))
            {
                ((Vector) registeredPlugins.get(containerID)).add(component);
            }
            else
            {
                Vector plugins = new Vector();
                plugins.add(component);
                registeredPlugins.put(containerID, plugins);
            }
            this.firePluginEvent(component, containerID,
                PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
        }
    }

    /**
     * Implements <code>UIService.addComponent(ContainerID, String, Object)
     * </code>.
     * For now this method only invokes addComponent(containerID, component).
     * 
     * @see UIService#addComponent(ContainerID, String, Object)
     */
    public void addComponent(ContainerID containerID, String constraint,
        Object component) throws ClassCastException, IllegalArgumentException
    {
        this.addComponent(containerID, component);
    }

    /**
     * 
     */
    public void addComponent(ContainerID containerID,
        ContactAwareComponent component) throws ClassCastException,
        IllegalArgumentException
    {
        if (!(component instanceof Component))
        {

            throw new ClassCastException(
                "The specified plugin is not a valid swing or awt component.");
        }

        this.addComponent(containerID, (Component) component);
    }

    /**
     * 
     */
    public void addComponent(ContainerID containerID, String constraint,
        ContactAwareComponent component) throws ClassCastException,
        IllegalArgumentException
    {
        this.addComponent(containerID, constraint, component);
    }

    /**
     * Implements <code>UISercie.getSupportedContainers</code>. Returns the
     * list of supported containers by this implementation .
     * 
     * @see UIService#getSupportedContainers()
     */
    public Iterator getSupportedContainers()
    {
        return Collections.unmodifiableList(supportedContainers).iterator();
    }

    /**
     * Implements getComponentsForConstraint in UIService interface.
     * 
     * @see UIService#getComponentsForContainer(ContainerID)
     */
    public Iterator getComponentsForContainer(ContainerID containerID)
        throws IllegalArgumentException
    {

        if (!supportedContainers.contains(containerID))
            throw new IllegalArgumentException(
                "The container that you specified is not "
                    + "supported by this UIService implementation.");

        Vector plugins = new Vector();

        Object o = registeredPlugins.get(containerID);

        if (o != null)
        {
            plugins = (Vector) o;
        }

        return plugins.iterator();
    }

    /**
     * Not yet implemented.
     * 
     * @see UIService#getConstraintsForContainer(ContainerID)
     */
    public Iterator getConstraintsForContainer(ContainerID containerID)
    {
        return null;
    }

    /**
     * Creates the corresponding PluginComponentEvent and notifies all
     * <tt>ContainerPluginListener</tt>s that a plugin component is added or
     * removed from the container.
     * 
     * @param pluginComponent the plugin component that is added to the
     *            container.
     * @param containerID the containerID that corresponds to the container
     *            where the component is added.
     * @param eventID one of the PLUGIN_COMPONENT_XXX static fields indicating
     *            the nature of the event.
     */
    private void firePluginEvent(Object pluginComponent,
        ContainerID containerID, int eventID)
    {
        PluginComponentEvent evt = new PluginComponentEvent(pluginComponent,
            containerID, eventID);

        logger.trace("Will dispatch the following plugin component event: "
            + evt);

        synchronized (pluginComponentListeners)
        {
            Iterator listeners = this.pluginComponentListeners.iterator();

            while (listeners.hasNext())
            {
                PluginComponentListener l = (PluginComponentListener) listeners
                    .next();

                switch (evt.getEventID())
                {
                case PluginComponentEvent.PLUGIN_COMPONENT_ADDED:
                    l.pluginComponentAdded(evt);
                    break;
                case PluginComponentEvent.PLUGIN_COMPONENT_REMOVED:
                    l.pluginComponentRemoved(evt);
                    break;
                default:
                    logger.error("Unknown event type " + evt.getEventID());
                }
            }
        }
    }

    /**
     * Implements <code>isVisible</code> in the UIService interface. Checks if
     * the main application window is visible.
     * 
     * @return <code>true</code> if main application window is visible,
     *         <code>false</code> otherwise
     * @see UIService#isVisible()
     */
    public boolean isVisible()
    {
        if (mainFrame.isVisible())
        {
            if (mainFrame.getExtendedState() == JFrame.ICONIFIED)
                return false;
            else
                return true;
        }
        else
            return false;
    }

    /**
     * Implements <code>setVisible</code> in the UIService interface. Shows or
     * hides the main application window depending on the parameter
     * <code>visible</code>.
     * 
     * @see UIService#setVisible(boolean)
     */
    public void setVisible(boolean visible)
    {
        this.mainFrame.setVisible(visible);
    }

    /**
     * Implements <code>minimize</code> in the UIService interface. Minimizes
     * the main application window.
     * 
     * @see UIService#minimize()
     */
    public void minimize()
    {
        this.mainFrame.setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements <code>maximize</code> in the UIService interface. Maximizes
     * the main application window.
     * 
     * @see UIService#maximize()
     */
    public void maximize()
    {
        this.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Implements <code>restore</code> in the UIService interface. Restores
     * the main application window.
     * 
     * @see UIService#restore()
     */
    public void restore()
    {
        if (mainFrame.isVisible())
        {

            if (mainFrame.getState() == JFrame.ICONIFIED)
                mainFrame.setState(JFrame.NORMAL);

            mainFrame.toFront();
        }
        else
            mainFrame.setVisible(true);
    }

    /**
     * Implements <code>resize</code> in the UIService interface. Resizes the
     * main application window.
     * 
     * @see UIService#resize(int, int)
     */
    public void resize(int width, int height)
    {
        this.mainFrame.setSize(width, height);
    }

    /**
     * Implements <code>move</code> in the UIService interface. Moves the main
     * application window to the point with coordinates - x, y.
     * 
     * @see UIService#move(int, int)
     */
    public void move(int x, int y)
    {
        this.mainFrame.setLocation(x, y);
    }

    /**
     * Implements the <code>UIService.setExitOnMainWindowClose</code>. Sets a
     * boolean property, which indicates whether the application should be
     * exited when the main application window is closed.
     */
    public void setExitOnMainWindowClose(boolean exitOnClose)
    {
        this.exitOnClose = exitOnClose;

        if (exitOnClose)
            mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        else
            mainFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    /**
     * Implements the <code>UIService.getExitOnMainWindowClose</code>.
     * Returns the boolean property, which indicates whether the application
     * should be exited when the main application window is closed.
     */
    public boolean getExitOnMainWindowClose()
    {
        return this.exitOnClose;
    }

    /**
     * Adds all <tt>ExportedWindow</tt>s to the list of application windows,
     * which could be used from other bundles. Once registered in the
     * <tt>UIService</tt> this window could be obtained through the
     * <tt>getExportedWindow(WindowID)</tt> method and could be shown,
     * hidden, resized, moved, etc.
     */   
    public void initExportedWindows()
    {
        AboutWindow aboutWindow = new AboutWindow(mainFrame);
        AddContactWizard addContactWizard = new AddContactWizard(mainFrame);
        
        exportedWindows.put(aboutWindow.getIdentifier(), aboutWindow);
        exportedWindows.put(configurationFrame.getIdentifier(), configurationFrame);
        exportedWindows.put(addContactWizard.getIdentifier(), addContactWizard);
    }
    
    /**
     * Registers the given <tt>ExportedWindow</tt> to the list of windows that
     * could be accessed from other bundles.
     * 
     * @param window the window to be exported
     */
    public void registerExportedWindow(ExportedWindow window)
    {
        exportedWindows.put(window.getIdentifier(), window);
    }
    
    /**
     * Sets the contact list service to this UI Service implementation.
     * @param contactList the MetaContactList service
     */
    public void setContactList(MetaContactListService contactList)
    {
        this.mainFrame.setContactList(contactList);        
    }
    
    public void addPluginComponentListener(PluginComponentListener l)
    {
        synchronized (pluginComponentListeners)
        {
            pluginComponentListeners.add(l);
        }
    }

    public void removePluginComponentListener(PluginComponentListener l)
    {
        synchronized (pluginComponentListeners)
        {
            pluginComponentListeners.remove(l);
        }
    }
    
    /**
     * Implements <code>getSupportedExportedWindows</code> in the UIService
     * interface. Returns an iterator over a set of all windows exported by
     * this implementation.
     * 
     * @see UIService#getSupportedExportedWindows()
     */
    public Iterator getSupportedExportedWindows()
    {
        return Collections.unmodifiableMap(exportedWindows).keySet().iterator();
    }

    /**
     * Implements the <code>getExportedWindow</code> in the UIService
     * interface. Returns the window corresponding to the given
     * <tt>WindowID</tt>.
     * 
     * @see UIService#getExportableComponent(WindowID)
     */
    public ExportedWindow getExportedWindow(WindowID windowID)
    {
        if (exportedWindows.containsKey(windowID))
        {
            return (ExportedWindow) exportedWindows.get(windowID);
        }
        return null;
    }
    
    /**
     * Implements the <code>UIService.isExportedWindowSupported</code> method.
     * Checks if there's an exported component for the given
     * <tt>WindowID</tt>.
     * 
     * @see UIService#isExportedWindowSupported(WindowID)
     */
    public boolean isExportedWindowSupported(WindowID windowID)
    {
        return exportedWindows.containsKey(windowID);
    }

    /**
     * Implements <code>getPopupDialog</code> in the UIService interface.
     * Returns a <tt>PopupDialog</tt> that could be used to show simple
     * messages, warnings, errors, etc.
     * 
     * @see UIService#getPopupDialog()
     */
    public PopupDialog getPopupDialog()
    {
        return this.popupDialog;
    }

    /**
     * Implements <code>getChat</code> in the UIService interface. If a
     * chat for the given contact exists already - returns it, otherwise
     * creates a new one.
     * 
     * @see UIService#getChat(Contact)
     */
    public Chat getChat(Contact contact)
    {
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(contact);

        ChatWindowManager chatWindowManager = mainFrame.getChatWindowManager();

        ChatPanel chatPanel = chatWindowManager.getContactChat(metaContact);

        return chatPanel;
    }   

    /**
     * Implements the <code>UIService.isContainerSupported</code> method.
     * Checks if the plugable container with the given ContainerID is supported
     * by this implementation.
     * 
     * @see UIService#isContainerSupported(ContainerID)
     */
    public boolean isContainerSupported(ContainerID containderID)
    {
        return supportedContainers.contains(containderID);
    }

    /**
     * Implements the <code>UIService.getAccountRegWizardContainer</code>
     * method. Returns the current implementation of the
     * <tt>AccountRegistrationWizardContainer</tt>.
     * 
     * @see UIService#getAccountRegWizardContainer()
     */
    public AccountRegistrationWizardContainer getAccountRegWizardContainer()
    {
        return this.wizardContainer;
    }

    /**
     * Implements the <code>UIService.getConfigurationWindow</code>. Returns
     * the current implementation of the <tt>ConfigurationWindow</tt>
     * interface.
     * 
     * @see UIService#getConfigurationWindow()
     */
    public ConfigurationWindow getConfigurationWindow()
    {
        return this.configurationFrame;
    }

    public ExportedWindow getAuthenticationWindow(
        ProtocolProviderService protocolProvider,
        String realm, UserCredentials userCredentials)
    {
        return new AuthenticationWindow(mainFrame, protocolProvider,
            realm, userCredentials);
    }

    /**
     * Returns the LoginManager.
     * @return the LoginManager
     */
    public LoginManager getLoginManager()
    {
        return loginManager;
    }

    /**
     * Returns the <tt>MainFrame</tt>. This is the class defining the main
     * application window.
     * 
     * @return the <tt>MainFrame</tt>
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }
    
    /**
     * The <tt>RunLogin</tt> implements the Runnable interface and is used to
     * shows the login windows in a seperate thread.
     */
    private class RunLoginGui implements Runnable {
        public void run() {
            loginManager.runLogin(mainFrame);
        }
    }
    
    /**
     * The <tt>RunApplication</tt> implements the Runnable interface and is used to
     * shows the main application window in a separate thread.
     */
    private class RunApplicationGui implements Runnable {
        public void run() {
            mainFrame.setVisible(true);
        }
    }

    /**
     * Sets the look&feel and the theme.
     */
    private void setDefaultThemePack() {

        SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
        SIPCommLookAndFeel.setCurrentTheme(new SIPCommDefaultTheme());

        // we need to set the UIDefaults class loader so that it may access
        // resources packed inside OSGI bundles
        UIManager.put("ClassLoader", getClass().getClassLoader());
        try {
            UIManager.setLookAndFeel(lf);
        } catch (UnsupportedLookAndFeelException e) {
            logger.error("The provided Look & Feel is not supported.", e);
        }
    }

}
