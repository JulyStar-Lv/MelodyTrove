use crate::PluginRuntimeError;
use quick_xml::{
    events::{BytesEnd, BytesStart, BytesText, Event},
    Reader, Writer,
};
use serde_json::{json, Map, Value};
use std::io::Cursor;
const MAX_XML: usize = 4 * 1024 * 1024;
const MAX_NODES: usize = 100_000;
const MAX_DEPTH: usize = 128;
#[derive(Clone)]
enum Node {
    Element(Element),
    Text(String),
}
#[derive(Clone)]
struct Element {
    name: String,
    attrs: Vec<(String, String)>,
    children: Vec<Node>,
}
pub fn root_attributes(xml: &str) -> Result<Value, PluginRuntimeError> {
    let root = parse(xml)?;
    Ok(Value::Object(
        root.attrs
            .into_iter()
            .map(|(k, v)| (k, Value::String(v)))
            .collect(),
    ))
}
pub fn find_elements(xml: &str, q: &Value) -> Result<Value, PluginRuntimeError> {
    let root = parse(xml)?;
    let tag = q.get("tag").and_then(Value::as_str).unwrap_or("");
    let attrs = q.get("attrs").and_then(Value::as_object);
    let mut out = vec![];
    walk(&root, &mut |e| {
        if (tag.is_empty() || e.name == tag) && matches_attrs(e, attrs) {
            out.push(element_json(e).unwrap_or(Value::Null))
        }
    });
    Ok(Value::Array(out))
}
pub fn replace_children_by_attr(xml: &str, o: &Value) -> Result<String, PluginRuntimeError> {
    let mut root = parse(xml)?;
    let tag = o.get("targetTag").and_then(Value::as_str).unwrap_or("");
    let key = o.get("keyAttr").and_then(Value::as_str).unwrap_or("");
    if tag.is_empty() || key.is_empty() {
        return Ok(xml.to_owned());
    }
    if let Some(a) = o.get("rootAttributes").and_then(Value::as_object) {
        for (k, v) in a {
            set_attr(&mut root, k, v.as_str().unwrap_or(""))
        }
    }
    let replacements = o.get("replacements").and_then(Value::as_object);
    walk_mut(&mut root, &mut |e| {
        if e.name != tag {
            return;
        }
        let Some(id) = attr(e, key).map(str::to_owned) else {
            return;
        };
        let Some(r) = replacements
            .and_then(|x| x.get(&id))
            .and_then(Value::as_object)
        else {
            return;
        };
        let value = r.get("value").and_then(Value::as_str).unwrap_or("");
        if r.get("mode").and_then(Value::as_str) == Some("xml") {
            if let Ok(fragment) = parse(&format!("<tt-root>{value}</tt-root>")) {
                e.children = fragment.children
            }
        } else {
            e.children = vec![Node::Text(value.to_owned())]
        }
    });
    serialize(&root)
}
pub fn remove_elements(xml: &str, q: &Value) -> Result<String, PluginRuntimeError> {
    let mut root = parse(xml)?;
    let tag = q.get("tag").and_then(Value::as_str).unwrap_or("");
    let attrs = q.get("attrs").and_then(Value::as_object);
    remove_in(&mut root, tag, attrs);
    walk_mut(&mut root, &mut |e| {
        if e.name == "translations" && !e.children.iter().any(|x| matches!(x, Node::Element(_))) {
            e.children.clear()
        }
    });
    serialize(&root)
}
fn parse(xml: &str) -> Result<Element, PluginRuntimeError> {
    if xml.len() > MAX_XML {
        return Err(err("XML exceeds size limit"));
    }
    let upper = xml.to_ascii_uppercase();
    if upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY") {
        return Err(err("DTD and entities are forbidden"));
    }
    let mut reader = Reader::from_str(xml);
    reader.config_mut().trim_text(false);
    let mut stack: Vec<Element> = vec![];
    let mut root = None;
    let mut nodes = 0;
    loop {
        match reader.read_event() {
            Ok(Event::Start(e)) => {
                nodes += 1;
                if nodes > MAX_NODES || stack.len() >= MAX_DEPTH {
                    return Err(err("XML complexity limit exceeded"));
                }
                stack.push(start(&reader, &e)?)
            }
            Ok(Event::Empty(e)) => {
                nodes += 1;
                let x = start(&reader, &e)?;
                if let Some(p) = stack.last_mut() {
                    p.children.push(Node::Element(x))
                } else {
                    root = Some(x)
                }
            }
            Ok(Event::Text(e)) => {
                if let Some(p) = stack.last_mut() {
                    nodes += 1;
                    p.children
                        .push(Node::Text(e.unescape().map_err(err)?.into_owned()))
                }
            }
            Ok(Event::CData(e)) => {
                if let Some(p) = stack.last_mut() {
                    nodes += 1;
                    p.children
                        .push(Node::Text(String::from_utf8_lossy(&e).into_owned()))
                }
            }
            Ok(Event::End(_)) => {
                let x = stack.pop().ok_or_else(|| err("unexpected XML end tag"))?;
                if let Some(p) = stack.last_mut() {
                    p.children.push(Node::Element(x))
                } else {
                    root = Some(x)
                }
            }
            Ok(Event::DocType(_)) => return Err(err("DTD is forbidden")),
            Ok(Event::Eof) => break,
            Ok(_) => {}
            Err(e) => return Err(err(e)),
        }
    }
    root.ok_or_else(|| err("XML root missing"))
}
fn start(reader: &Reader<&[u8]>, e: &BytesStart) -> Result<Element, PluginRuntimeError> {
    let name = String::from_utf8_lossy(e.name().as_ref()).into_owned();
    let attrs = e
        .attributes()
        .with_checks(true)
        .map(|a| {
            let a = a.map_err(err)?;
            Ok((
                String::from_utf8_lossy(a.key.as_ref()).into_owned(),
                a.decode_and_unescape_value(reader.decoder())
                    .map_err(err)?
                    .into_owned(),
            ))
        })
        .collect::<Result<Vec<_>, PluginRuntimeError>>()?;
    Ok(Element {
        name,
        attrs,
        children: vec![],
    })
}
fn serialize(root: &Element) -> Result<String, PluginRuntimeError> {
    let mut w = Writer::new(Cursor::new(Vec::new()));
    write_element(&mut w, root)?;
    String::from_utf8(w.into_inner().into_inner()).map_err(err)
}
fn write_element(w: &mut Writer<Cursor<Vec<u8>>>, e: &Element) -> Result<(), PluginRuntimeError> {
    let mut s = BytesStart::new(&e.name);
    for (k, v) in &e.attrs {
        s.push_attribute((k.as_str(), v.as_str()))
    }
    w.write_event(Event::Start(s)).map_err(err)?;
    for n in &e.children {
        match n {
            Node::Element(x) => write_element(w, x)?,
            Node::Text(x) => w.write_event(Event::Text(BytesText::new(x))).map_err(err)?,
        }
    }
    w.write_event(Event::End(BytesEnd::new(&e.name)))
        .map_err(err)
}
fn walk(e: &Element, f: &mut impl FnMut(&Element)) {
    f(e);
    for n in &e.children {
        if let Node::Element(x) = n {
            walk(x, f)
        }
    }
}
fn walk_mut(e: &mut Element, f: &mut impl FnMut(&mut Element)) {
    f(e);
    for n in &mut e.children {
        if let Node::Element(x) = n {
            walk_mut(x, f)
        }
    }
}
fn remove_in(e: &mut Element, tag: &str, attrs: Option<&Map<String, Value>>) {
    e.children.retain(
        |n| !matches!(n,Node::Element(x)if(tag.is_empty()||x.name==tag)&&matches_attrs(x,attrs)),
    );
    for n in &mut e.children {
        if let Node::Element(x) = n {
            remove_in(x, tag, attrs)
        }
    }
}
fn matches_attrs(e: &Element, a: Option<&Map<String, Value>>) -> bool {
    a.map_or(true, |a| {
        a.iter()
            .all(|(k, v)| attr(e, k) == Some(v.as_str().unwrap_or("")))
    })
}
fn attr<'a>(e: &'a Element, k: &str) -> Option<&'a str> {
    e.attrs
        .iter()
        .find(|(n, _)| n == k)
        .map(|(_, v)| v.as_str())
}
fn set_attr(e: &mut Element, k: &str, v: &str) {
    if let Some(x) = e.attrs.iter_mut().find(|(n, _)| n == k) {
        x.1 = v.into()
    } else {
        e.attrs.push((k.into(), v.into()))
    }
}
fn element_json(e: &Element) -> Result<Value, PluginRuntimeError> {
    let attrs = e
        .attrs
        .iter()
        .map(|(k, v)| (k.clone(), Value::String(v.clone())))
        .collect();
    let text = text_content(e);
    let inner = e
        .children
        .iter()
        .map(serialize_node)
        .collect::<Result<Vec<_>, _>>()?
        .join("");
    let children = e
        .children
        .iter()
        .filter_map(|n| {
            if let Node::Element(x) = n {
                Some(element_json(x))
            } else {
                None
            }
        })
        .collect::<Result<Vec<_>, _>>()?;
    Ok(
        json!({"tag":e.name,"attrs":Value::Object(attrs),"text":text,"innerXml":inner,"children":children}),
    )
}
fn serialize_node(n: &Node) -> Result<String, PluginRuntimeError> {
    match n {
        Node::Element(e) => serialize(e),
        Node::Text(t) => {
            let mut w = Writer::new(Cursor::new(Vec::new()));
            w.write_event(Event::Text(BytesText::new(t))).map_err(err)?;
            String::from_utf8(w.into_inner().into_inner()).map_err(err)
        }
    }
}
fn text_content(e: &Element) -> String {
    e.children
        .iter()
        .map(|n| match n {
            Node::Text(x) => x.clone(),
            Node::Element(x) => text_content(x),
        })
        .collect()
}
fn err(e: impl ToString) -> PluginRuntimeError {
    PluginRuntimeError::HostApi(e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn xml_operations() {
        let x = r#"<tt xml:lang="en"><translation key="a"><p>Hello</p></translation><translation key="b">Bye</translation></tt>"#;
        assert_eq!(root_attributes(x).unwrap()["xml:lang"], "en");
        let found = find_elements(x, &json!({"tag":"translation","attrs":{"key":"a"}})).unwrap();
        assert_eq!(found[0]["text"], "Hello");
        let replaced=replace_children_by_attr(x,&json!({"targetTag":"translation","keyAttr":"key","replacements":{"a":{"mode":"text","value":"Hi & bye"}}})).unwrap();
        assert!(replaced.contains("Hi &amp; bye"));
        let removed =
            remove_elements(&replaced, &json!({"tag":"translation","attrs":{"key":"b"}})).unwrap();
        assert!(!removed.contains("Bye"));
    }
    #[test]
    fn rejects_dtd() {
        assert!(root_attributes("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///x'>]><x>&e;</x>").is_err())
    }
}
