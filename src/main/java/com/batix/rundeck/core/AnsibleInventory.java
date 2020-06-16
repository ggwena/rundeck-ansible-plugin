package com.batix.rundeck.core;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AnsibleInventory {

  public class AnsibleInventoryHosts {

    protected Map<String, JsonObject> hosts = new HashMap<String, JsonObject>();
    protected Map<String, AnsibleInventoryHosts> children = new HashMap<String, AnsibleInventoryHosts>();

    public AnsibleInventoryHosts addHost(String nodeName) {
      hosts.put(nodeName, new JsonObject());
      return this;
    }

    public AnsibleInventoryHosts addHost(String nodeName, String host, Map<String, String> attributes) {
      attributes.put("ansible_host", host);

      // convert back 'String attribute.value' to JsonArray or JsonObject, when not a Primitive
      JsonObject attributesJson = new JsonObject();
      for (String attribute : attributes.keySet()) {
        System.out.println("attributes: " + attribute + "="+ attributes.get(attribute));
        // JsonElement attributeJson = new JsonParser().parse(attributes.get(attribute));
        JsonElement attributeJson = new Gson().fromJson(attributes.get(attribute), JsonElement.class);
        attributesJson.add(attribute, attributeJson);
      }

      hosts.put(nodeName, attributesJson);
      return this;
    }

    public AnsibleInventoryHosts getOrAddChildHostGroup(String groupName) {
      children.putIfAbsent(groupName, new AnsibleInventoryHosts());
      return children.get(groupName);
    }
  }

  protected AnsibleInventoryHosts all = new AnsibleInventoryHosts();

  public AnsibleInventory addHost(String nodeName, String host, Map<String, String> attributes) {
    // Remove attributes that are reserved in Ansible
    String[] reserved = { "hostvars", "group_names", "groups", "environment" };
    for (String r: reserved){
      attributes.remove(r);
    }
    all.addHost(nodeName, host, attributes);
    // Create Ansible groups by attribute
    // Group by osFamily is needed for windows hosts setup
    String[] attributeGroups = { "osFamily", "tags" };
    for (String g: attributeGroups) {
      if (attributes.containsKey(g)) {
        String[] groupNames = attributes.get(g).toLowerCase().split(",");
        for (String groupName: groupNames) {
          all.getOrAddChildHostGroup(groupName.trim()).addHost(nodeName);
        }
      }
    }
    return this;
  }
}
